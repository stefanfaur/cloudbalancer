"""Integration tests against a *running* CloudBalancer deployment.

Skipped entirely unless CB_STRESS_URL is set, e.g.:

    CB_STRESS_URL=http://localhost uv run pytest tests/test_integration.py -v

Credentials default to admin/admin; override with CB_STRESS_USERNAME /
CB_STRESS_PASSWORD. These tests are read-mostly (login + queries) plus a
single short SIMULATED task submission — safe to run against the dev
compose or the two-machine deployment.
"""

from __future__ import annotations

import os
import time

import pytest

from cloudbalancer_stress.client import CloudBalancerClient, TaskDescriptor
from cloudbalancer_stress.monitor import SETTLED_STATES, compute_since_floor

CB_STRESS_URL = os.environ.get("CB_STRESS_URL")

pytestmark = pytest.mark.skipif(
    not CB_STRESS_URL,
    reason="CB_STRESS_URL not set; integration tests need a running deployment",
)


@pytest.fixture(scope="module")
def client():
    with CloudBalancerClient(
        base_url=CB_STRESS_URL,
        username=os.environ.get("CB_STRESS_USERNAME", "admin"),
        password=os.environ.get("CB_STRESS_PASSWORD", "admin"),
    ) as c:
        c.login()
        yield c


def test_login_succeeds(client: CloudBalancerClient) -> None:
    # the fixture already logged in; a token must be present
    assert client._access_token  # noqa: SLF001


def test_get_agents(client: CloudBalancerClient) -> None:
    agents = client.get_agents()
    # zero agents is valid (DOCKER runtime mode); the shape must parse
    for agent in agents:
        assert agent.agent_id
        assert agent.total_cpu_cores >= 0
        assert isinstance(agent.supported_executors, list)


def test_get_scaling_status(client: CloudBalancerClient) -> None:
    status = client.get_scaling_status()
    assert status.worker_count >= 0
    assert status.active_worker_count >= 0


def test_cluster_metrics_best_effort(client: CloudBalancerClient) -> None:
    # must not raise regardless of whether the aggregator is reachable
    metrics = client.get_cluster_metrics()
    if metrics is not None:
        assert metrics.raw  # parsed something


def test_submit_and_observe_simulated_task(client: CloudBalancerClient) -> None:
    """Submit one 2s SIMULATED task and poll it to a settled state."""
    envelope = client.submit_task(
        TaskDescriptor(executor_type="SIMULATED", execution_spec={"durationMs": 2000})
    )
    assert envelope.id
    assert envelope.submitted_at

    since = compute_since_floor(envelope.submitted_at)
    deadline = time.monotonic() + 120
    state = envelope.state
    while time.monotonic() < deadline:
        matching = [t for t in client.list_tasks(since=since) if t.id == envelope.id]
        if matching:
            state = matching[0].state
            if state in SETTLED_STATES:
                break
        time.sleep(2)
    assert state == "COMPLETED", f"task {envelope.id} ended in {state}"
