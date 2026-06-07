"""Unit tests for report exit codes, classification, and rendering."""

from __future__ import annotations

import pytest
from rich.console import Console

from cloudbalancer_stress.monitor import RunStateView, TrackedTask
from cloudbalancer_stress.report import Report, percentile
from cloudbalancer_stress.scenarios import build as build_scenario


def make_task(
    task_id: str,
    state: str,
    executor: str = "SHELL",
    latency: float | None = None,
) -> TrackedTask:
    return TrackedTask(
        task_id=task_id,
        workload_name="log-crunch",
        executor_type=executor,
        state=state,
        submitted_at="2026-06-07T12:00:00Z",
        submitted_monotonic=0.0,
        settled_monotonic=latency,
    )


def make_view(tasks: list[TrackedTask]) -> RunStateView:
    return RunStateView(
        tasks={t.task_id: t for t in tasks},
        agents=[],
        scaling=None,
        cluster_metrics=None,
        events=[],
        scaling_events=[],
        status="RUNNING",
        status_detail="",
        submission_done=True,
    )


SMOKE = build_scenario("smoke")  # allow_failures=False
CHAOS = build_scenario("chaos")  # allow_failures=True


def test_all_completed_exits_zero() -> None:
    view = make_view([make_task("t-1", "COMPLETED", latency=5.0)])
    report = Report(view, SMOKE, timed_out=False)
    assert report.exit_code == 0
    assert report.verdict.startswith("PASS")


def test_no_tasks_exits_one() -> None:
    report = Report(make_view([]), SMOKE, timed_out=False)
    assert report.exit_code == 1
    assert "no tasks" in report.verdict


def test_timeout_with_unsettled_exits_one() -> None:
    view = make_view(
        [make_task("t-1", "COMPLETED", latency=5.0), make_task("t-2", "RUNNING")]
    )
    report = Report(view, SMOKE, timed_out=True)
    assert report.exit_code == 1
    assert "unsettled" in report.verdict


def test_unexpected_failure_exits_one() -> None:
    view = make_view([make_task("t-1", "DEAD_LETTERED", latency=9.0)])
    report = Report(view, SMOKE, timed_out=False)
    assert report.exit_code == 1
    assert "unexpected failure" in report.verdict


def test_expected_failures_pass_when_scenario_allows() -> None:
    view = make_view(
        [
            make_task("t-1", "COMPLETED", latency=5.0),
            make_task("t-2", "DEAD_LETTERED", latency=30.0),
            make_task("t-3", "SUBMIT_FAILED", latency=0.0),
        ]
    )
    report = Report(view, CHAOS, timed_out=False)
    assert report.exit_code == 0
    assert "expected by scenario" in report.verdict


def test_unsettled_without_timeout_flag_still_fails() -> None:
    # defensive: an unsettled task means the run did not finish cleanly
    view = make_view([make_task("t-1", "FAILED")])  # FAILED is non-settled
    report = Report(view, CHAOS, timed_out=False)
    assert report.exit_code == 1


def test_percentile_nearest_rank() -> None:
    values = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]
    assert percentile(values, 50) == 5.0
    assert percentile(values, 95) == 10.0
    assert percentile([3.0], 50) == 3.0
    with pytest.raises(ValueError):
        percentile([], 50)


def test_build_renders_with_and_without_log_fetcher() -> None:
    view = make_view(
        [
            make_task("t-1", "COMPLETED", "SHELL", latency=5.0),
            make_task("t-2", "COMPLETED", "PYTHON", latency=8.0),
            make_task("t-3", "DEAD_LETTERED", "SIMULATED", latency=20.0),
        ]
    )
    report = Report(view, CHAOS, timed_out=False)
    console = Console(width=100, record=True)

    console.print(report.build())  # no fetcher: no sample-output section
    plain = console.export_text()
    assert "per-executor results" in plain
    assert "sample output" not in plain

    def fetch_logs(task_id: str) -> dict[str, str]:
        if task_id == "t-2":
            raise RuntimeError("boom")
        return {"stdout": "line1\nline2", "stderr": ""}

    console = Console(width=100, record=True)
    console.print(report.build(fetch_logs))
    plain = console.export_text()
    assert "sample output" in plain
    assert "line1" in plain  # SHELL sample fetched
    assert "logs unavailable" in plain  # PYTHON fetch failed, best-effort
