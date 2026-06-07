"""Scenario presets — named load shapes rendered to submission plans.

A scenario factory takes a seeded random.Random and returns a
SubmissionPlan: an ordered list of (offset_s, workload, size, priority,
executionPolicy) entries. Same seed -> identical plan, so runs are
reproducible and comparable.

Chaos-only failure templates (fail-python, fail-shell, sim-timeout) live
here rather than in the public workload catalog: they are intentionally
broken tasks, not realistic workloads.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from typing import Any, Callable

from .workloads import WORKLOADS, Workload, by_executor

HIGH_PRIORITY_RATIO = 0.15
_SIZE_CHOICES = ("S", "S", "S", "M", "M", "L")  # ~50/35/15 weighting

# executionPolicy that forces TIMED_OUT then DEAD_LETTERED (10s timeout
# against a fixed 20s task, one retry).
TIMEOUT_POLICY = {
    "maxRetries": 1,
    "timeoutSeconds": 10,
    "retryBackoffStrategy": "FIXED",
    "failureAction": "RETRY",
}


@dataclass(frozen=True)
class PlannedTask:
    """One scheduled submission within a scenario."""

    offset_s: float
    workload_name: str
    size: str
    priority: str | None = None
    execution_policy: dict[str, Any] | None = None


@dataclass(frozen=True)
class SubmissionPlan:
    """A fully rendered scenario: what to submit and when."""

    name: str
    description: str
    allow_failures: bool
    tasks: tuple[PlannedTask, ...]

    @property
    def duration_s(self) -> float:
        return max((t.offset_s for t in self.tasks), default=0.0)

    @property
    def executor_types(self) -> set[str]:
        return {resolve_workload(t.workload_name).executor_type for t in self.tasks}


# ---------------------------------------------------------------------------
# Chaos-only failure templates
# ---------------------------------------------------------------------------

_FAIL_PYTHON_SCRIPT = """\
import sys

print("processing batch 1 of 3")
print("encountered unrecoverable input error", file=sys.stderr)
sys.exit(1)
"""

_CHAOS_EXTRAS: dict[str, Workload] = {
    w.name: w
    for w in (
        Workload(
            "fail-python",
            "PYTHON",
            "python script that exits 1",
            lambda size, rng: {
                "script": _FAIL_PYTHON_SCRIPT,
                "networkAccessRequired": False,
            },
        ),
        Workload(
            "fail-shell",
            "SHELL",
            "shell command that exits nonzero",
            lambda size, rng: {
                "command": 'echo "starting nightly job"; ls /nonexistent_path_xyz; exit 3'
            },
        ),
        Workload(
            "sim-timeout",
            "SIMULATED",
            "20s simulated task run under a 10s timeout policy",
            lambda size, rng: {"durationMs": 20000},
        ),
    )
}


def resolve_workload(name: str) -> Workload:
    """Look up a workload in the public catalog or the chaos extras."""
    if name in WORKLOADS:
        return WORKLOADS[name]
    if name in _CHAOS_EXTRAS:
        return _CHAOS_EXTRAS[name]
    raise KeyError(f"unknown workload {name!r}")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

# sim-flaky is chaos-only; keep it out of the general pools.
_GENERAL_POOL_BY_EXECUTOR = {
    "SHELL": [w.name for w in by_executor("SHELL")],
    "PYTHON": [w.name for w in by_executor("PYTHON")],
    "DOCKER": [w.name for w in by_executor("DOCKER")],
    "SIMULATED": ["sim-short", "sim-long"],
}
def _priority(rng: random.Random) -> str | None:
    """~15% of tasks get HIGH priority to make queue-jumping visible."""
    return "HIGH" if rng.random() < HIGH_PRIORITY_RATIO else None


def _pick(rng: random.Random, executor_type: str) -> str:
    return rng.choice(_GENERAL_POOL_BY_EXECUTOR[executor_type])


def _size(rng: random.Random) -> str:
    return rng.choice(_SIZE_CHOICES)


def _burst_mix(rng: random.Random, offset_s: float = 0.0) -> list[PlannedTask]:
    """~30 tasks at one offset, mixed 30/30/20/20 shell/python/docker/sim."""
    mix = [("SHELL", 9), ("PYTHON", 9), ("DOCKER", 6), ("SIMULATED", 6)]
    tasks = [
        PlannedTask(offset_s, _pick(rng, executor), _size(rng), _priority(rng))
        for executor, count in mix
        for _ in range(count)
    ]
    rng.shuffle(tasks)
    return tasks


# ---------------------------------------------------------------------------
# Scenario factories
# ---------------------------------------------------------------------------


def smoke(rng: random.Random) -> SubmissionPlan:
    """One quick task per executor type at t=0 — routing sanity check."""
    tasks = tuple(
        PlannedTask(0.0, name, "S")
        for name in ("csv-report", "montecarlo-pi", "checksum-farm", "sim-short")
    )
    return SubmissionPlan(
        name="smoke",
        description="1 task per executor type at t=0",
        allow_failures=False,
        tasks=tasks,
    )


def burst(rng: random.Random) -> SubmissionPlan:
    """~30 tasks at t=0 to provoke QUEUE_PRESSURE scale-up."""
    return SubmissionPlan(
        name="burst",
        description="30 tasks at t=0, mix 30/30/20/20 shell/python/docker/sim",
        allow_failures=False,
        tasks=tuple(_burst_mix(rng)),
    )


def ramp(rng: random.Random) -> SubmissionPlan:
    """Submission rate climbs 1/15s -> 1/8s -> 1/3s over ~6 minutes."""
    phases = [(0.0, 120.0, 15.0), (120.0, 240.0, 8.0), (240.0, 360.0, 3.0)]
    tasks: list[PlannedTask] = []
    for start, end, interval in phases:
        offset = start
        while offset < end:
            executor = rng.choice(list(_GENERAL_POOL_BY_EXECUTOR))
            tasks.append(
                PlannedTask(offset, _pick(rng, executor), _size(rng), _priority(rng))
            )
            offset += interval
    return SubmissionPlan(
        name="ramp",
        description="rate climbs 1/15s -> 1/8s -> 1/3s over ~6 min",
        allow_failures=False,
        tasks=tuple(tasks),
    )


def steady(rng: random.Random) -> SubmissionPlan:
    """One task every 6s for ~5 minutes — sustained routing."""
    tasks = []
    for i in range(50):
        executor = rng.choice(list(_GENERAL_POOL_BY_EXECUTOR))
        tasks.append(
            PlannedTask(i * 6.0, _pick(rng, executor), _size(rng), _priority(rng))
        )
    return SubmissionPlan(
        name="steady",
        description="1 task per 6s for ~5 min",
        allow_failures=False,
        tasks=tuple(tasks),
    )


def chaos(rng: random.Random) -> SubmissionPlan:
    """Burst mix + flaky/failing/timing-out tasks — exercises retries,
    FAILED->QUEUED recycling, TIMED_OUT, and DEAD_LETTERED."""
    tasks = _burst_mix(rng)
    # flaky simulated tasks (40% failure per attempt)
    for _ in range(4):
        tasks.append(
            PlannedTask(rng.uniform(0.0, 10.0), "sim-flaky", _size(rng), _priority(rng))
        )
    # deterministic failures
    tasks.append(PlannedTask(rng.uniform(0.0, 10.0), "fail-python", "S"))
    tasks.append(PlannedTask(rng.uniform(0.0, 10.0), "fail-shell", "S"))
    # 20s task under a 10s timeout policy -> TIMED_OUT -> DEAD_LETTERED
    tasks.append(
        PlannedTask(
            rng.uniform(0.0, 10.0),
            "sim-timeout",
            "S",
            execution_policy=dict(TIMEOUT_POLICY),
        )
    )
    tasks.sort(key=lambda t: t.offset_s)
    return SubmissionPlan(
        name="chaos",
        description="burst mix + flaky, failing, and timing-out tasks",
        allow_failures=True,
        tasks=tuple(tasks),
    )


SCENARIOS: dict[str, Callable[[random.Random], SubmissionPlan]] = {
    "smoke": smoke,
    "burst": burst,
    "ramp": ramp,
    "steady": steady,
    "chaos": chaos,
}

DEFAULT_SEED = 1234


def build(name: str, seed: int | None = None) -> SubmissionPlan:
    """Render a named scenario with the given (or default) seed."""
    try:
        factory = SCENARIOS[name]
    except KeyError:
        raise KeyError(f"unknown scenario {name!r}; known: {sorted(SCENARIOS)}") from None
    return factory(random.Random(DEFAULT_SEED if seed is None else seed))
