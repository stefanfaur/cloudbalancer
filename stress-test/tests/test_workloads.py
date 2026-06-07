"""Unit tests for the workload catalog: blocklist safety + spec shapes."""

from __future__ import annotations

import random

import pytest

from cloudbalancer_stress.workloads import (
    SHELL_BLOCKLIST,
    SIZES,
    WORKLOADS,
    BlocklistViolation,
    by_executor,
    by_name,
    validate_shell_command,
)


def test_catalog_has_all_eleven_workloads() -> None:
    assert len(WORKLOADS) == 11
    assert len(by_executor("SHELL")) == 3
    assert len(by_executor("PYTHON")) == 3
    assert len(by_executor("DOCKER")) == 2
    assert len(by_executor("SIMULATED")) == 3


def test_by_name_unknown_raises() -> None:
    with pytest.raises(KeyError, match="unknown workload"):
        by_name("nope")


def test_unknown_size_raises() -> None:
    with pytest.raises(ValueError, match="unknown size"):
        by_name("sim-short").build_spec("XL", random.Random(1))


@pytest.mark.parametrize("workload", by_executor("SHELL"), ids=lambda w: w.name)
@pytest.mark.parametrize("size", SIZES)
@pytest.mark.parametrize("seed", [0, 1, 42, 12345])
def test_every_shell_command_is_blocklist_clean(workload, size, seed) -> None:
    """The worker blocklist is a raw substring match (notably bare 'dd')."""
    spec = workload.build_spec(size, random.Random(seed))
    command = spec["command"]
    for blocked in SHELL_BLOCKLIST:
        assert blocked not in command, (
            f"{workload.name}/{size} contains blocked substring {blocked!r}"
        )


@pytest.mark.parametrize(
    "bad_command",
    [
        "rm -rf / --no-preserve-root",
        "shutdown -h now",
        "echo hi && reboot",
        "mkfs.ext4 /dev/sda1",
        "head -c 1024 /dev/zero | dd of=/tmp/x",
        "echo padding",  # 'dd' hidden inside an ordinary word
        ":(){ :|:& };:",
    ],
)
def test_validate_shell_command_rejects_blocked(bad_command: str) -> None:
    with pytest.raises(BlocklistViolation):
        validate_shell_command(bad_command)


def test_validate_shell_command_accepts_clean() -> None:
    validate_shell_command("echo hello | sort | uniq -c")


@pytest.mark.parametrize("workload", by_executor("SHELL"), ids=lambda w: w.name)
def test_shell_specs_have_command_string(workload) -> None:
    spec = workload.build_spec("M", random.Random(7))
    assert isinstance(spec["command"], str)
    assert spec["command"].strip()


@pytest.mark.parametrize("workload", by_executor("PYTHON"), ids=lambda w: w.name)
@pytest.mark.parametrize("size", SIZES)
def test_python_specs_compile_and_disable_network(workload, size) -> None:
    spec = workload.build_spec(size, random.Random(7))
    assert spec["networkAccessRequired"] is False
    compile(spec["script"], f"<{workload.name}>", "exec")  # syntax check


@pytest.mark.parametrize("workload", by_executor("DOCKER"), ids=lambda w: w.name)
def test_docker_specs_shape(workload) -> None:
    spec = workload.build_spec("M", random.Random(7))
    assert spec["image"] == "alpine:3.20"
    assert isinstance(spec["command"], list)  # argv array, NOT a shell string
    assert spec["command"][0] == "sh"
    assert spec["memoryLimitBytes"] == 256 * 1024 * 1024
    assert spec["cpuCount"] == 1
    assert spec["networkDisabled"] is True


def test_simulated_durations_within_documented_ranges() -> None:
    rng = random.Random(3)
    for _ in range(20):
        short = by_name("sim-short").build_spec("S", rng)
        assert 2000 <= short["durationMs"] <= 5000
        long = by_name("sim-long").build_spec("L", rng)
        assert 10000 <= long["durationMs"] <= 20000


def test_sim_flaky_sets_fail_probability() -> None:
    spec = by_name("sim-flaky").build_spec("M", random.Random(3))
    assert spec["failProbability"] == 0.4
    assert "durationMs" in spec


def test_specs_are_reproducible_for_same_seed() -> None:
    for workload in WORKLOADS.values():
        first = workload.build_spec("M", random.Random(99))
        second = workload.build_spec("M", random.Random(99))
        assert first == second, f"{workload.name} not reproducible"


def test_simulated_duration_is_integer_milliseconds() -> None:
    """The validator rejects a missing durationMs; it must be an int."""
    for name in ("sim-short", "sim-long", "sim-flaky"):
        spec = by_name(name).build_spec("M", random.Random(5))
        assert isinstance(spec["durationMs"], int)
