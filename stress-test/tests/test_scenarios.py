"""Unit tests for scenario presets: structure, counts, reproducibility."""

from __future__ import annotations

import random
from collections import Counter

import pytest

from cloudbalancer_stress.scenarios import (
    SCENARIOS,
    TIMEOUT_POLICY,
    build,
    resolve_workload,
)
from cloudbalancer_stress.workloads import validate_shell_command


def test_registry_has_all_five_scenarios() -> None:
    assert sorted(SCENARIOS) == ["burst", "chaos", "ramp", "smoke", "steady"]


def test_build_unknown_scenario_raises() -> None:
    with pytest.raises(KeyError, match="unknown scenario"):
        build("hurricane")


@pytest.mark.parametrize("name", sorted(SCENARIOS))
def test_every_planned_workload_resolves(name: str) -> None:
    plan = build(name)
    for task in plan.tasks:
        workload = resolve_workload(task.workload_name)
        spec = workload.build_spec(task.size, random.Random(1))
        assert spec  # builds without raising (incl. SHELL blocklist check)


@pytest.mark.parametrize("name", sorted(SCENARIOS))
def test_plans_are_reproducible_for_same_seed(name: str) -> None:
    assert build(name, seed=7) == build(name, seed=7)


def test_different_seeds_differ() -> None:
    assert build("burst", seed=1) != build("burst", seed=2)


def test_smoke_covers_all_executors_at_t0() -> None:
    plan = build("smoke")
    assert len(plan.tasks) == 4
    assert all(t.offset_s == 0.0 for t in plan.tasks)
    assert plan.executor_types == {"SHELL", "PYTHON", "DOCKER", "SIMULATED"}
    assert plan.allow_failures is False


def test_burst_is_30_tasks_at_t0_with_documented_mix() -> None:
    plan = build("burst")
    assert len(plan.tasks) == 30
    assert all(t.offset_s == 0.0 for t in plan.tasks)
    mix = Counter(resolve_workload(t.workload_name).executor_type for t in plan.tasks)
    assert mix == {"SHELL": 9, "PYTHON": 9, "DOCKER": 6, "SIMULATED": 6}
    assert all(t.workload_name != "sim-flaky" for t in plan.tasks)  # chaos-only


def test_ramp_rate_climbs() -> None:
    plan = build("ramp")
    offsets = [t.offset_s for t in plan.tasks]
    assert offsets == sorted(offsets)
    assert plan.duration_s < 360.0
    first_phase = sum(1 for o in offsets if o < 120.0)
    last_phase = sum(1 for o in offsets if o >= 240.0)
    assert first_phase == 8  # 120s at 1/15s
    assert last_phase == 40  # 120s at 1/3s
    assert last_phase > first_phase


def test_steady_is_one_task_per_6s_for_5_min() -> None:
    plan = build("steady")
    assert len(plan.tasks) == 50
    offsets = [t.offset_s for t in plan.tasks]
    assert offsets[0] == 0.0
    assert offsets[-1] == 294.0
    assert all(b - a == 6.0 for a, b in zip(offsets, offsets[1:]))


def test_chaos_includes_failure_templates_and_allows_failures() -> None:
    plan = build("chaos")
    assert plan.allow_failures is True
    names = Counter(t.workload_name for t in plan.tasks)
    assert names["sim-flaky"] == 4
    assert names["fail-python"] == 1
    assert names["fail-shell"] == 1
    assert names["sim-timeout"] == 1
    assert len(plan.tasks) == 37  # 30 burst mix + 7 chaos extras
    offsets = [t.offset_s for t in plan.tasks]
    assert offsets == sorted(offsets)


def test_chaos_timeout_task_carries_timeout_policy() -> None:
    plan = build("chaos")
    timeout_tasks = [t for t in plan.tasks if t.workload_name == "sim-timeout"]
    assert timeout_tasks[0].execution_policy == TIMEOUT_POLICY
    # the spec is a fixed 20s duration, far above the 10s timeout
    spec = resolve_workload("sim-timeout").build_spec("S", random.Random(1))
    assert spec["durationMs"] == 20000
    assert TIMEOUT_POLICY["timeoutSeconds"] * 1000 < spec["durationMs"]


def test_fail_shell_is_blocklist_clean() -> None:
    spec = resolve_workload("fail-shell").build_spec("S", random.Random(1))
    validate_shell_command(spec["command"])  # must not raise


def test_fail_python_compiles() -> None:
    spec = resolve_workload("fail-python").build_spec("S", random.Random(1))
    compile(spec["script"], "<fail-python>", "exec")


def test_high_priority_ratio_is_roughly_15_percent() -> None:
    # aggregate across several large plans to smooth the noise
    total = 0
    high = 0
    for seed in range(10):
        plan = build("ramp", seed=seed)
        total += len(plan.tasks)
        high += sum(1 for t in plan.tasks if t.priority == "HIGH")
    ratio = high / total
    assert 0.08 < ratio < 0.25


def test_priorities_are_valid_values() -> None:
    for name in SCENARIOS:
        for task in build(name).tasks:
            assert task.priority in (None, "LOW", "NORMAL", "HIGH")
