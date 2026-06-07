"""Report — final run summary, latency percentiles, and exit code.

Exit code semantics:
- 0: every submitted task settled, and any non-COMPLETED outcomes are
     acceptable for the scenario (allow_failures).
- 1: run timeout (unsettled tasks), unexpected failures, or no tasks.

A *failure* is any settled task that did not COMPLETE: DEAD_LETTERED,
CANCELLED, or local SUBMIT_FAILED.
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Callable

from rich.console import Group, RenderableType
from rich.panel import Panel
from rich.table import Table
from rich.text import Text

from .monitor import RunStateView, TrackedTask
from .scenarios import SubmissionPlan

_EXECUTOR_ORDER = ("SHELL", "PYTHON", "DOCKER", "SIMULATED")

# fetch_logs(task_id) -> {"stdout": ..., "stderr": ...}
LogFetcher = Callable[[str], dict[str, str]]


def percentile(values: list[float], p: float) -> float:
    """Nearest-rank percentile over a non-empty value list."""
    if not values:
        raise ValueError("percentile of empty list")
    ordered = sorted(values)
    rank = max(1, math.ceil(p / 100.0 * len(ordered)))
    return ordered[rank - 1]


@dataclass
class ExecutorSummary:
    executor_type: str
    submitted: int = 0
    completed: int = 0
    failed: int = 0  # settled non-COMPLETED, non-DEAD_LETTERED (incl. SUBMIT_FAILED)
    dead_lettered: int = 0
    unsettled: int = 0
    latencies: list[float] | None = None


class Report:
    def __init__(
        self,
        view: RunStateView,
        plan: SubmissionPlan,
        timed_out: bool,
    ) -> None:
        self._view = view
        self._plan = plan
        self._timed_out = timed_out

    # -- classification -------------------------------------------------------

    def _summaries(self) -> dict[str, ExecutorSummary]:
        summaries: dict[str, ExecutorSummary] = {}
        for task in self._view.tasks.values():
            summary = summaries.setdefault(
                task.executor_type,
                ExecutorSummary(task.executor_type, latencies=[]),
            )
            summary.submitted += 1
            if task.state == "COMPLETED":
                summary.completed += 1
            elif task.state == "DEAD_LETTERED":
                summary.dead_lettered += 1
            elif task.settled:  # CANCELLED, SUBMIT_FAILED
                summary.failed += 1
            else:
                summary.unsettled += 1
            if task.latency_s is not None and task.state == "COMPLETED":
                assert summary.latencies is not None
                summary.latencies.append(task.latency_s)
        return summaries

    @property
    def failure_count(self) -> int:
        return sum(
            1
            for task in self._view.tasks.values()
            if task.settled and task.state != "COMPLETED"
        )

    @property
    def unsettled_count(self) -> int:
        return sum(1 for task in self._view.tasks.values() if not task.settled)

    # -- exit code -------------------------------------------------------------

    @property
    def exit_code(self) -> int:
        if not self._view.tasks:
            return 1
        if self._timed_out or self.unsettled_count:
            return 1
        if self.failure_count and not self._plan.allow_failures:
            return 1
        return 0

    @property
    def verdict(self) -> str:
        if not self._view.tasks:
            return "FAIL — no tasks were submitted"
        if self._timed_out or self.unsettled_count:
            return (
                f"FAIL — run timeout with {self.unsettled_count} unsettled task(s)"
            )
        if self.failure_count and not self._plan.allow_failures:
            return f"FAIL — {self.failure_count} unexpected failure(s)"
        if self.failure_count:
            return (
                f"PASS — all tasks settled; {self.failure_count} failure(s) "
                f"expected by scenario '{self._plan.name}'"
            )
        return "PASS — all tasks completed"

    # -- rendering ---------------------------------------------------------------

    def _summary_table(self) -> Table:
        table = Table(title="per-executor results", header_style="bold dim")
        table.add_column("executor", style="bold")
        for column in ("submitted", "completed", "failed", "dead-lettered", "unsettled"):
            table.add_column(column, justify="right")
        for column in ("p50", "p95", "max"):
            table.add_column(f"{column} (s)", justify="right")

        summaries = self._summaries()
        ordered = [e for e in _EXECUTOR_ORDER if e in summaries] + sorted(
            set(summaries) - set(_EXECUTOR_ORDER)
        )
        for executor in ordered:
            s = summaries[executor]
            latencies = s.latencies or []
            table.add_row(
                executor,
                str(s.submitted),
                Text(str(s.completed), style="green" if s.completed else "dim"),
                Text(str(s.failed), style="red" if s.failed else "dim"),
                Text(str(s.dead_lettered), style="red" if s.dead_lettered else "dim"),
                Text(str(s.unsettled), style="dark_orange" if s.unsettled else "dim"),
                f"{percentile(latencies, 50):.1f}" if latencies else "—",
                f"{percentile(latencies, 95):.1f}" if latencies else "—",
                f"{max(latencies):.1f}" if latencies else "—",
            )
        return table

    def _scaling_timeline(self) -> RenderableType:
        if not self._view.scaling_events:
            return Text("no scaling decisions observed", style="dim")
        lines = [
            Text(f"  {event.timestamp}  {event.render()}",
                 style="green" if event.action == "SCALE_UP" else "dark_orange")
            for event in self._view.scaling_events
        ]
        return Group(Text("scaling timeline", style="bold"), *lines)

    def _sample_tasks(self) -> list[TrackedTask]:
        """One COMPLETED task per executor, for log sampling."""
        chosen: dict[str, TrackedTask] = {}
        for task in self._view.tasks.values():
            if task.state == "COMPLETED" and task.executor_type not in chosen:
                chosen[task.executor_type] = task
        return [chosen[e] for e in _EXECUTOR_ORDER if e in chosen]

    def _sample_logs(self, fetch_logs: LogFetcher) -> RenderableType:
        sections: list[RenderableType] = []
        for task in self._sample_tasks():
            try:
                logs = fetch_logs(task.task_id)
            except Exception as exc:  # best-effort: never fail the report
                sections.append(
                    Text(
                        f"{task.executor_type} logs unavailable: {exc}",
                        style="dim",
                    )
                )
                continue
            stdout = (logs.get("stdout") or "").strip()
            excerpt = "\n".join(stdout.splitlines()[:10]) or "(empty stdout)"
            sections.append(
                Panel(
                    excerpt,
                    title=f"{task.executor_type} · {task.workload_name} · {task.task_id[:8]}",
                    title_align="left",
                    border_style="dim",
                )
            )
        if not sections:
            return Text("no completed tasks to sample logs from", style="dim")
        return Group(*sections)

    def build(self, fetch_logs: LogFetcher | None = None) -> RenderableType:
        verdict_style = "bold green" if self.exit_code == 0 else "bold red"
        sections: list[RenderableType] = [
            Text(self.verdict, style=verdict_style),
            Text(),
            self._summary_table(),
            Text(),
            self._scaling_timeline(),
        ]
        if fetch_logs is not None:
            sections.extend([Text(), Text("sample output", style="bold"), self._sample_logs(fetch_logs)])
        return Panel(
            Group(*sections),
            title=f"[bold]report · {self._plan.name}[/bold]",
            border_style="green" if self.exit_code == 0 else "red",
        )
