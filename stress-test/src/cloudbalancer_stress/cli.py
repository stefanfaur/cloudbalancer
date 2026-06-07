"""CLI entry point — argparse wiring and the run loop.

Run loop: login -> preflight/bootstrap -> submission thread walks the
scenario schedule -> monitor polls until everything settles or
--run-timeout expires -> final report + exit code.
"""

from __future__ import annotations

import argparse
import os
import random
import sys
import threading
import time
from collections import Counter

import httpx
from rich.console import Console
from rich.live import Live

from .client import AuthError, CloudBalancerClient, TaskDescriptor, Throttled
from .dashboard import Dashboard
from .monitor import Monitor, MonitorAborted
from .report import Report
from .scenarios import DEFAULT_SEED, SCENARIOS, SubmissionPlan, build, resolve_workload

SUBMIT_RETRIES = 2  # retried twice, then recorded SUBMIT_FAILED


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="cloudbalancer-stress",
        description="Stress-test a running CloudBalancer deployment.",
        epilog=(
            "Point --url at the nginx-fronted origin (port 80), not the "
            "dispatcher's :8080 — cluster metrics are served by the separate "
            "metrics-aggregator and only nginx routes both under one origin."
        ),
    )
    parser.add_argument(
        "scenario",
        choices=sorted(SCENARIOS),
        help="named load scenario to run",
    )
    parser.add_argument(
        "--url",
        default=os.environ.get("CB_STRESS_URL", "http://localhost"),
        help="deployment origin (default: %(default)s; env CB_STRESS_URL)",
    )
    parser.add_argument(
        "--username",
        default=os.environ.get("CB_STRESS_USERNAME", "admin"),
        help="login username (default: %(default)s; env CB_STRESS_USERNAME)",
    )
    parser.add_argument(
        "--password",
        default=os.environ.get("CB_STRESS_PASSWORD", "admin"),
        help="login password (default: admin; env CB_STRESS_PASSWORD)",
    )
    parser.add_argument(
        "--poll-interval",
        type=float,
        default=2.0,
        help="seconds between status polls (default: %(default)s)",
    )
    parser.add_argument(
        "--run-timeout",
        type=float,
        default=900.0,
        help="abort the run after this many seconds (default: %(default)s)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help=f"scenario RNG seed for reproducible plans (default: {DEFAULT_SEED})",
    )
    parser.add_argument(
        "--no-bootstrap",
        action="store_true",
        help="never trigger a bootstrap SCALE_UP at cold start (observe only)",
    )
    return parser


def _planned_by_executor(plan: SubmissionPlan) -> dict[str, int]:
    return dict(
        Counter(resolve_workload(t.workload_name).executor_type for t in plan.tasks)
    )


# ---------------------------------------------------------------------------
# Submission thread
# ---------------------------------------------------------------------------


def _submission_loop(
    client: CloudBalancerClient,
    plan: SubmissionPlan,
    monitor: Monitor,
    rng: random.Random,
    stop: threading.Event,
) -> None:
    """Walk the scenario schedule on a monotonic clock and submit tasks."""
    start = time.monotonic()
    try:
        for planned in plan.tasks:
            target = start + planned.offset_s
            while not stop.is_set():
                remaining = target - time.monotonic()
                if remaining <= 0:
                    break
                stop.wait(min(0.2, remaining))
            if stop.is_set():
                return

            workload = resolve_workload(planned.workload_name)
            spec = workload.build_spec(planned.size, rng)
            descriptor = TaskDescriptor(
                executor_type=workload.executor_type,
                execution_spec=spec,
                priority=planned.priority,
                execution_policy=planned.execution_policy,
            )

            failures = 0
            while not stop.is_set():
                try:
                    envelope = client.submit_task(descriptor)
                    monitor.register_submission(envelope, workload.name)
                    break
                except Throttled as throttled:
                    # rate limiting is never a submit failure — pause and retry
                    stop.wait(throttled.retry_after)
                except (httpx.TransportError, httpx.HTTPStatusError, AuthError):
                    failures += 1
                    if failures > SUBMIT_RETRIES:
                        monitor.register_submit_failure(
                            workload.name, workload.executor_type
                        )
                        break
                    stop.wait(1.0)
    finally:
        monitor.mark_submission_done()


# ---------------------------------------------------------------------------
# Preflight / bootstrap
# ---------------------------------------------------------------------------


def _preflight(
    client: CloudBalancerClient,
    plan: SubmissionPlan,
    no_bootstrap: bool,
    console: Console,
) -> bool:
    """Login, agent/worker checks, capability warning, optional bootstrap.

    Returns False when the run cannot proceed.
    """
    try:
        client.login()
    except AuthError as exc:
        console.print(f"[bold red]login failed:[/bold red] {exc}")
        return False
    except httpx.TransportError as exc:
        console.print(f"[bold red]cannot reach {client.base_url}:[/bold red] {exc}")
        return False

    try:
        agents = client.get_agents()
        scaling = client.get_scaling_status()
    except (httpx.TransportError, httpx.HTTPStatusError, Throttled) as exc:
        console.print(f"[bold red]preflight failed:[/bold red] {exc}")
        return False

    if not agents and scaling.worker_count == 0:
        console.print(
            "[bold red]no alive agents — start (or restart) a slave agent "
            "first.[/bold red] A blind SCALE_UP trigger would no-op silently."
        )
        return False

    if agents:
        supported = set().union(*(a.supported_executors for a in agents))
        missing = plan.executor_types - supported
        if missing:
            console.print(
                f"[yellow]warning:[/yellow] no registered agent supports "
                f"{', '.join(sorted(missing))} — those tasks will queue "
                f"until a capable worker appears"
            )

    if scaling.worker_count == 0:
        if no_bootstrap:
            console.print(
                "[dim]--no-bootstrap: zero workers; observing only[/dim]"
            )
        else:
            target = max(agents, key=lambda a: a.available_cpu_cores)
            try:
                client.trigger_scaling("SCALE_UP", 1, agent_id=target.agent_id)
                console.print(
                    f"[cyan]bootstrap:[/cyan] SCALE_UP 1 on {target.agent_id} "
                    f"({target.available_cpu_cores:.1f} CPU available)"
                )
            except (httpx.TransportError, httpx.HTTPStatusError, Throttled) as exc:
                console.print(f"[yellow]bootstrap trigger failed:[/yellow] {exc}")

    return True


# ---------------------------------------------------------------------------
# Run loop
# ---------------------------------------------------------------------------


def _run(args: argparse.Namespace, console: Console) -> int:
    plan = build(args.scenario, args.seed)
    spec_rng = random.Random(DEFAULT_SEED if args.seed is None else args.seed)

    console.print(
        f"[bold]cloudbalancer-stress[/bold] · scenario [cyan]{plan.name}[/cyan] "
        f"({len(plan.tasks)} tasks over ~{plan.duration_s:.0f}s) · {args.url}"
    )

    with CloudBalancerClient(args.url, args.username, args.password) as client:
        if not _preflight(client, plan, args.no_bootstrap, console):
            return 1

        monitor = Monitor(client, poll_interval=args.poll_interval)
        dashboard = Dashboard(plan.name, args.url, _planned_by_executor(plan))
        stop = threading.Event()
        submitter = threading.Thread(
            target=_submission_loop,
            args=(client, plan, monitor, spec_rng, stop),
            name="submission",
            daemon=True,
        )

        timed_out = False
        abort_reason: str | None = None
        deadline = time.monotonic() + args.run_timeout
        submitter.start()

        try:
            with Live(
                dashboard.render(monitor.snapshot()),
                console=console,
                refresh_per_second=4,
            ) as live:
                while True:
                    delay = monitor.poll_cycle()
                    live.update(dashboard.render(monitor.snapshot()))

                    view = monitor.snapshot()
                    if view.submission_done and view.all_settled:
                        break
                    now = time.monotonic()
                    if now >= deadline:
                        timed_out = True
                        break

                    # tick the dashboard while waiting for the next poll
                    next_poll = min(now + delay, deadline)
                    while (remaining := next_poll - time.monotonic()) > 0:
                        time.sleep(min(0.25, remaining))
                        live.update(dashboard.render(monitor.snapshot()))
        except MonitorAborted as exc:
            timed_out = True
            abort_reason = str(exc)
        except AuthError as exc:
            timed_out = True
            abort_reason = f"authentication lost mid-run: {exc}"
        except KeyboardInterrupt:
            timed_out = True
            abort_reason = "interrupted"
        finally:
            stop.set()
            submitter.join(timeout=5.0)

        if abort_reason:
            console.print(f"[bold red]run aborted:[/bold red] {abort_reason}")

        report = Report(monitor.snapshot(), plan, timed_out)

        def fetch_logs(task_id: str) -> dict[str, str]:
            return client.get_task_logs(task_id)

        console.print(report.build(fetch_logs))
        return report.exit_code


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    console = Console()
    return _run(args, console)


if __name__ == "__main__":
    sys.exit(main())
