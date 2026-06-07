"""Monitor — REST polling + snapshot diffing into a derived event timeline.

The monitor owns all mutable run state. The submission thread registers
submissions; the main loop calls poll_cycle(); the dashboard and report
read consistent copies via snapshot(). A single lock guards everything
(monitor writes, dashboard/report read).

Diffing rules:
- task state changes        -> TaskTransitionEvent (worker from last executionHistory entry)
- new scaling lastDecision  -> ScalingDecisionEvent (null-guarded, deduped on timestamp)
- worker/agent count change -> CountChangeEvent

A task is *settled* in COMPLETED / CANCELLED / DEAD_LETTERED (or local
SUBMIT_FAILED). FAILED and TIMED_OUT are non-settled: they may recycle to
QUEUED until they succeed or reach DEAD_LETTERED.
"""

from __future__ import annotations

import threading
import time
from collections import deque
from dataclasses import dataclass, replace
from datetime import datetime, timedelta
from typing import Callable

import httpx

from .client import (
    Agent,
    AuthError,
    ClusterMetrics,
    CloudBalancerClient,
    ScalingStatus,
    TaskEnvelope,
    Throttled,
)

SETTLED_STATES = frozenset({"COMPLETED", "CANCELLED", "DEAD_LETTERED"})
SUBMIT_FAILED = "SUBMIT_FAILED"  # local-only terminal state

# clock-skew safety margin subtracted from the first server submittedAt
SINCE_FLOOR_MARGIN_S = 5

# error-handling knobs (design: backoff cap 30s, abort after 2 min)
BACKOFF_BASE_S = 2.0
BACKOFF_CAP_S = 30.0
CONTINUOUS_FAILURE_ABORT_S = 120.0

METRICS_EVERY_N_CYCLES = 3


class MonitorAborted(Exception):
    """Polling failed continuously for longer than the abort window."""


def compute_since_floor(submitted_at: str) -> str:
    """First submission's server submittedAt minus the safety margin.

    Never derived from the client clock — the filter compares against
    server-assigned timestamps.
    """
    instant = datetime.fromisoformat(submitted_at.replace("Z", "+00:00"))
    floored = instant - timedelta(seconds=SINCE_FLOOR_MARGIN_S)
    return floored.isoformat().replace("+00:00", "Z")


# ---------------------------------------------------------------------------
# Events
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class TaskTransitionEvent:
    task_id: str
    workload_name: str
    from_state: str
    to_state: str
    worker_id: str | None
    at_monotonic: float


@dataclass(frozen=True)
class ScalingDecisionEvent:
    action: str
    reason: str
    trigger_type: str
    previous_worker_count: int
    new_worker_count: int
    timestamp: str

    def render(self) -> str:
        return (
            f"{self.trigger_type} {self.action} "
            f"{self.previous_worker_count}→{self.new_worker_count} ({self.reason})"
        )


@dataclass(frozen=True)
class CountChangeEvent:
    kind: str  # "workers" | "agents"
    previous: int
    current: int


Event = TaskTransitionEvent | ScalingDecisionEvent | CountChangeEvent


# ---------------------------------------------------------------------------
# Tracked state
# ---------------------------------------------------------------------------


@dataclass
class TrackedTask:
    """One submitted task as the monitor sees it."""

    task_id: str
    workload_name: str
    executor_type: str
    state: str
    submitted_at: str  # server-assigned, ISO-8601
    submitted_monotonic: float
    settled_monotonic: float | None = None
    worker_id: str | None = None

    @property
    def settled(self) -> bool:
        return self.state in SETTLED_STATES or self.state == SUBMIT_FAILED

    @property
    def latency_s(self) -> float | None:
        """Submit -> settled wall time (local clocks; includes up to one
        poll interval of observation lag)."""
        if self.settled_monotonic is None:
            return None
        return self.settled_monotonic - self.submitted_monotonic


@dataclass
class RunStateView:
    """Consistent copy of the run state for dashboard/report rendering."""

    tasks: dict[str, TrackedTask]
    agents: list[Agent]
    scaling: ScalingStatus | None
    cluster_metrics: ClusterMetrics | None
    events: list[Event]
    scaling_events: list[ScalingDecisionEvent]
    status: str  # RUNNING | THROTTLED | RECONNECTING
    status_detail: str
    submission_done: bool

    @property
    def state_counts(self) -> dict[str, int]:
        counts: dict[str, int] = {}
        for task in self.tasks.values():
            counts[task.state] = counts.get(task.state, 0) + 1
        return counts

    @property
    def all_settled(self) -> bool:
        return bool(self.tasks) and all(t.settled for t in self.tasks.values())


# ---------------------------------------------------------------------------
# Monitor
# ---------------------------------------------------------------------------


class Monitor:
    def __init__(
        self,
        client: CloudBalancerClient,
        poll_interval: float = 2.0,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self._client = client
        self._poll_interval = poll_interval
        self._clock = clock
        self._lock = threading.Lock()

        self._tasks: dict[str, TrackedTask] = {}
        self._agents: list[Agent] = []
        self._scaling: ScalingStatus | None = None
        self._metrics: ClusterMetrics | None = None
        self._events: deque[Event] = deque(maxlen=200)
        self._scaling_events: list[ScalingDecisionEvent] = []
        self._seen_decision_timestamps: set[str] = set()
        self._since_floor: str | None = None
        self._status = "RUNNING"
        self._status_detail = ""
        self._submission_done = False

        self._cycle = 0
        self._failure_window_start: float | None = None
        self._consecutive_failures = 0
        self._submit_failed_seq = 0

    # -- submission-side registration (called from the submission thread) --

    def register_submission(self, envelope: TaskEnvelope, workload_name: str) -> None:
        with self._lock:
            if self._since_floor is None and envelope.submitted_at:
                self._since_floor = compute_since_floor(envelope.submitted_at)
            self._tasks[envelope.id] = TrackedTask(
                task_id=envelope.id,
                workload_name=workload_name,
                executor_type=envelope.executor_type,
                state=envelope.state,
                submitted_at=envelope.submitted_at,
                submitted_monotonic=self._clock(),
            )

    def register_submit_failure(self, workload_name: str, executor_type: str) -> None:
        with self._lock:
            self._submit_failed_seq += 1
            task_id = f"submit-failed-{self._submit_failed_seq}"
            now = self._clock()
            self._tasks[task_id] = TrackedTask(
                task_id=task_id,
                workload_name=workload_name,
                executor_type=executor_type,
                state=SUBMIT_FAILED,
                submitted_at="",
                submitted_monotonic=now,
                settled_monotonic=now,
            )

    def mark_submission_done(self) -> None:
        with self._lock:
            self._submission_done = True

    # -- polling -----------------------------------------------------------

    def poll_cycle(self) -> float:
        """One polling cycle. Returns the recommended sleep in seconds.

        Raises MonitorAborted after CONTINUOUS_FAILURE_ABORT_S of
        uninterrupted failure, and lets AuthError propagate (fatal).
        """
        with self._lock:
            since_floor = self._since_floor
            fetch_metrics = self._cycle % METRICS_EVERY_N_CYCLES == 0
            self._cycle += 1

        try:
            envelopes = (
                self._client.list_tasks(since=since_floor) if since_floor else []
            )
            agents = self._client.get_agents()
            scaling = self._client.get_scaling_status()
            metrics = self._client.get_cluster_metrics() if fetch_metrics else None
        except Throttled as throttled:
            with self._lock:
                self._status = "THROTTLED"
                self._status_detail = f"rate limited; resuming in {throttled.retry_after:.0f}s"
            return throttled.retry_after
        except AuthError:
            raise
        except (httpx.TransportError, httpx.HTTPStatusError, ValueError) as exc:
            return self._record_failure(exc)

        self.apply_snapshot(envelopes, agents, scaling, metrics)
        return self._poll_interval

    def _record_failure(self, exc: Exception) -> float:
        now = self._clock()
        with self._lock:
            if self._failure_window_start is None:
                self._failure_window_start = now
            elif now - self._failure_window_start >= CONTINUOUS_FAILURE_ABORT_S:
                raise MonitorAborted(
                    f"polling failed continuously for "
                    f"{now - self._failure_window_start:.0f}s: {exc}"
                ) from exc
            self._consecutive_failures += 1
            self._status = "RECONNECTING"
            self._status_detail = str(exc)
            backoff = min(
                BACKOFF_CAP_S, BACKOFF_BASE_S * 2 ** (self._consecutive_failures - 1)
            )
        return backoff

    # -- snapshot diffing (pure state mutation; unit-testable) -------------

    def apply_snapshot(
        self,
        envelopes: list[TaskEnvelope],
        agents: list[Agent],
        scaling: ScalingStatus | None,
        metrics: ClusterMetrics | None,
    ) -> None:
        now = self._clock()
        with self._lock:
            self._failure_window_start = None
            self._consecutive_failures = 0
            self._status = "RUNNING"
            self._status_detail = ""

            # task transitions — only tasks we submitted
            for envelope in envelopes:
                tracked = self._tasks.get(envelope.id)
                if tracked is None:
                    continue
                if envelope.state != tracked.state:
                    self._events.append(
                        TaskTransitionEvent(
                            task_id=envelope.id,
                            workload_name=tracked.workload_name,
                            from_state=tracked.state,
                            to_state=envelope.state,
                            worker_id=envelope.last_worker_id,
                            at_monotonic=now,
                        )
                    )
                    tracked.state = envelope.state
                    if tracked.settled and tracked.settled_monotonic is None:
                        tracked.settled_monotonic = now
                    elif not tracked.settled:
                        # FAILED -> QUEUED recycling reopens the task
                        tracked.settled_monotonic = None
                if envelope.last_worker_id:
                    tracked.worker_id = envelope.last_worker_id

            # scaling decisions — null-guarded, deduped on timestamp
            if scaling is not None and scaling.last_decision is not None:
                decision = scaling.last_decision
                if decision.timestamp not in self._seen_decision_timestamps:
                    self._seen_decision_timestamps.add(decision.timestamp)
                    event = ScalingDecisionEvent(
                        action=decision.action,
                        reason=decision.reason,
                        trigger_type=decision.trigger_type,
                        previous_worker_count=decision.previous_worker_count,
                        new_worker_count=decision.new_worker_count,
                        timestamp=decision.timestamp,
                    )
                    self._events.append(event)
                    self._scaling_events.append(event)

            # worker / agent count changes
            if scaling is not None:
                previous = self._scaling.worker_count if self._scaling else None
                if previous is not None and previous != scaling.worker_count:
                    self._events.append(
                        CountChangeEvent("workers", previous, scaling.worker_count)
                    )
                self._scaling = scaling

            previous_agents = len(self._agents)
            if self._agents and previous_agents != len(agents):
                self._events.append(
                    CountChangeEvent("agents", previous_agents, len(agents))
                )
            self._agents = agents

            if metrics is not None:
                self._metrics = metrics

    # -- readers -------------------------------------------------------------

    def snapshot(self) -> RunStateView:
        with self._lock:
            return RunStateView(
                tasks={tid: replace(t) for tid, t in self._tasks.items()},
                agents=list(self._agents),
                scaling=self._scaling,
                cluster_metrics=self._metrics,
                events=list(self._events),
                scaling_events=list(self._scaling_events),
                status=self._status,
                status_detail=self._status_detail,
                submission_done=self._submission_done,
            )

    @property
    def since_floor(self) -> str | None:
        with self._lock:
            return self._since_floor
