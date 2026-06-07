"""Unit tests for the monitor: snapshot diffing, settling, error handling."""

from __future__ import annotations

import httpx
import pytest

from cloudbalancer_stress.client import (
    Agent,
    ScalingStatus,
    TaskEnvelope,
    Throttled,
)
from cloudbalancer_stress.monitor import (
    BACKOFF_CAP_S,
    CountChangeEvent,
    Monitor,
    MonitorAborted,
    ScalingDecisionEvent,
    TaskTransitionEvent,
    compute_since_floor,
)


class FakeClock:
    def __init__(self) -> None:
        self.now = 1000.0

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


def make_envelope(task_id: str, state: str, worker_id: str | None = None) -> TaskEnvelope:
    history = [{"workerId": worker_id, "state": state}] if worker_id else []
    return TaskEnvelope(
        id=task_id,
        state=state,
        submitted_at="2026-06-07T12:00:00Z",
        executor_type="SHELL",
        execution_history=history,
    )


def make_scaling(
    worker_count: int = 1, decision: dict | None = None
) -> ScalingStatus:
    return ScalingStatus.from_json(
        {"workerCount": worker_count, "lastDecision": decision}
    )


def make_monitor(clock: FakeClock | None = None) -> tuple[Monitor, FakeClock]:
    clock = clock or FakeClock()
    monitor = Monitor(client=None, poll_interval=2.0, clock=clock)  # type: ignore[arg-type]
    return monitor, clock


def submit(monitor: Monitor, task_id: str, state: str = "SUBMITTED") -> None:
    monitor.register_submission(make_envelope(task_id, state), workload_name="log-crunch")


# -- since floor -------------------------------------------------------------


def test_compute_since_floor_subtracts_safety_margin() -> None:
    assert compute_since_floor("2026-06-07T12:00:00Z") == "2026-06-07T11:59:55Z"


def test_since_floor_set_from_first_submission_only() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    first_floor = monitor.since_floor
    monitor.register_submission(
        TaskEnvelope(
            id="t-2",
            state="SUBMITTED",
            submitted_at="2026-06-07T13:00:00Z",
            executor_type="SHELL",
        ),
        workload_name="log-crunch",
    )
    assert monitor.since_floor == first_floor == "2026-06-07T11:59:55Z"


# -- task transitions ----------------------------------------------------------


def test_state_change_emits_transition_with_worker_attribution() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot(
        [make_envelope("t-1", "RUNNING", worker_id="w-7")], [], None, None
    )
    view = monitor.snapshot()
    transitions = [e for e in view.events if isinstance(e, TaskTransitionEvent)]
    assert len(transitions) == 1
    assert transitions[0].from_state == "SUBMITTED"
    assert transitions[0].to_state == "RUNNING"
    assert transitions[0].worker_id == "w-7"
    assert view.tasks["t-1"].state == "RUNNING"
    assert view.tasks["t-1"].worker_id == "w-7"


def test_unchanged_state_emits_nothing() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot([make_envelope("t-1", "SUBMITTED")], [], None, None)
    assert monitor.snapshot().events == []


def test_untracked_tasks_are_ignored() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot([make_envelope("other", "RUNNING")], [], None, None)
    view = monitor.snapshot()
    assert view.events == []
    assert "other" not in view.tasks


def test_unknown_states_tracked_verbatim() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot([make_envelope("t-1", "QUARANTINED")], [], None, None)
    view = monitor.snapshot()
    assert view.tasks["t-1"].state == "QUARANTINED"
    assert not view.tasks["t-1"].settled


# -- settling & latency --------------------------------------------------------


@pytest.mark.parametrize("state", ["COMPLETED", "CANCELLED", "DEAD_LETTERED"])
def test_terminal_states_settle(state: str) -> None:
    monitor, clock = make_monitor()
    submit(monitor, "t-1")
    clock.advance(12.5)
    monitor.apply_snapshot([make_envelope("t-1", state)], [], None, None)
    task = monitor.snapshot().tasks["t-1"]
    assert task.settled
    assert task.latency_s == 12.5


@pytest.mark.parametrize("state", ["FAILED", "TIMED_OUT", "RUNNING", "QUEUED"])
def test_non_terminal_states_do_not_settle(state: str) -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot([make_envelope("t-1", state)], [], None, None)
    task = monitor.snapshot().tasks["t-1"]
    assert not task.settled
    assert task.latency_s is None


def test_failed_to_queued_recycling_keeps_task_open() -> None:
    monitor, _ = make_monitor()
    submit(monitor, "t-1")
    monitor.apply_snapshot([make_envelope("t-1", "FAILED")], [], None, None)
    monitor.apply_snapshot([make_envelope("t-1", "QUEUED")], [], None, None)
    monitor.apply_snapshot([make_envelope("t-1", "COMPLETED")], [], None, None)
    view = monitor.snapshot()
    assert view.tasks["t-1"].settled
    transitions = [e for e in view.events if isinstance(e, TaskTransitionEvent)]
    assert [t.to_state for t in transitions] == ["FAILED", "QUEUED", "COMPLETED"]


def test_all_settled_requires_tasks_and_all_terminal() -> None:
    monitor, _ = make_monitor()
    assert not monitor.snapshot().all_settled  # no tasks yet
    submit(monitor, "t-1")
    submit(monitor, "t-2")
    monitor.apply_snapshot([make_envelope("t-1", "COMPLETED")], [], None, None)
    assert not monitor.snapshot().all_settled
    monitor.apply_snapshot([make_envelope("t-2", "DEAD_LETTERED")], [], None, None)
    assert monitor.snapshot().all_settled


def test_submit_failure_is_terminal_immediately() -> None:
    monitor, _ = make_monitor()
    monitor.register_submit_failure("log-crunch", "SHELL")
    view = monitor.snapshot()
    assert view.all_settled
    [task] = view.tasks.values()
    assert task.state == "SUBMIT_FAILED"
    assert task.settled


# -- scaling decisions -----------------------------------------------------------


def test_scaling_decision_emitted_once_and_deduped_on_timestamp() -> None:
    monitor, _ = make_monitor()
    decision = {
        "action": "SCALE_UP",
        "reason": "queue pressure",
        "triggerType": "QUEUE_PRESSURE",
        "previousWorkerCount": 1,
        "newWorkerCount": 2,
        "timestamp": "2026-06-07T12:01:00Z",
    }
    monitor.apply_snapshot([], [], make_scaling(1, decision), None)
    monitor.apply_snapshot([], [], make_scaling(2, decision), None)  # same timestamp
    view = monitor.snapshot()
    scaling_events = [e for e in view.events if isinstance(e, ScalingDecisionEvent)]
    assert len(scaling_events) == 1
    assert view.scaling_events == scaling_events
    assert scaling_events[0].render() == "QUEUE_PRESSURE SCALE_UP 1→2 (queue pressure)"


def test_null_decision_is_guarded() -> None:
    monitor, _ = make_monitor()
    monitor.apply_snapshot([], [], make_scaling(0, None), None)
    assert monitor.snapshot().scaling_events == []


def test_new_decision_timestamp_emits_again() -> None:
    monitor, _ = make_monitor()
    base = {
        "action": "SCALE_UP",
        "reason": "r",
        "triggerType": "QUEUE_PRESSURE",
        "previousWorkerCount": 1,
        "newWorkerCount": 2,
    }
    monitor.apply_snapshot([], [], make_scaling(1, {**base, "timestamp": "T1"}), None)
    monitor.apply_snapshot([], [], make_scaling(2, {**base, "timestamp": "T2"}), None)
    assert len(monitor.snapshot().scaling_events) == 2


# -- count changes ---------------------------------------------------------------


def test_worker_count_change_event() -> None:
    monitor, _ = make_monitor()
    monitor.apply_snapshot([], [], make_scaling(1), None)
    monitor.apply_snapshot([], [], make_scaling(3), None)
    changes = [e for e in monitor.snapshot().events if isinstance(e, CountChangeEvent)]
    assert changes == [CountChangeEvent("workers", 1, 3)]


def test_agent_count_change_event() -> None:
    monitor, _ = make_monitor()
    agent = Agent.from_json({"agentId": "a-1"})
    monitor.apply_snapshot([], [agent], None, None)
    monitor.apply_snapshot([], [agent, Agent.from_json({"agentId": "a-2"})], None, None)
    changes = [e for e in monitor.snapshot().events if isinstance(e, CountChangeEvent)]
    assert changes == [CountChangeEvent("agents", 1, 2)]


# -- poll_cycle error handling ------------------------------------------------


class StubClient:
    """Duck-typed client whose poll methods raise a scripted exception."""

    def __init__(self, error: Exception) -> None:
        self.error = error

    def list_tasks(self, since: str, limit: int = 500):
        raise self.error

    def get_agents(self):
        raise self.error

    def get_scaling_status(self):
        raise self.error

    def get_cluster_metrics(self):
        raise self.error


def test_poll_cycle_throttled_sets_banner_and_returns_retry_after() -> None:
    clock = FakeClock()
    monitor = Monitor(StubClient(Throttled(60.0)), clock=clock)  # type: ignore[arg-type]
    delay = monitor.poll_cycle()
    assert delay == 60.0
    assert monitor.snapshot().status == "THROTTLED"


def test_poll_cycle_backoff_grows_and_caps() -> None:
    clock = FakeClock()
    error = httpx.ConnectError("connection refused")
    monitor = Monitor(StubClient(error), clock=clock)  # type: ignore[arg-type]
    delays = []
    for _ in range(6):
        delays.append(monitor.poll_cycle())
        clock.advance(10.0)
    assert delays == [2.0, 4.0, 8.0, 16.0, 30.0, 30.0]
    assert max(delays) == BACKOFF_CAP_S
    assert monitor.snapshot().status == "RECONNECTING"


def test_poll_cycle_aborts_after_two_minutes_of_failure() -> None:
    clock = FakeClock()
    monitor = Monitor(
        StubClient(httpx.ConnectError("down")), clock=clock
    )  # type: ignore[arg-type]
    monitor.poll_cycle()  # opens the failure window
    clock.advance(121.0)
    with pytest.raises(MonitorAborted):
        monitor.poll_cycle()


def test_successful_snapshot_resets_failure_window_and_status() -> None:
    clock = FakeClock()
    monitor = Monitor(StubClient(httpx.ConnectError("down")), clock=clock)  # type: ignore[arg-type]
    monitor.poll_cycle()
    assert monitor.snapshot().status == "RECONNECTING"
    monitor.apply_snapshot([], [], None, None)
    assert monitor.snapshot().status == "RUNNING"
