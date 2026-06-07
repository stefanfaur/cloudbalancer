"""Unit tests for CloudBalancerClient using httpx.MockTransport."""

from __future__ import annotations

import json
from typing import Callable

import httpx
import pytest

from cloudbalancer_stress.client import (
    Agent,
    AuthError,
    CloudBalancerClient,
    ScalingStatus,
    TaskDescriptor,
    TaskEnvelope,
    Throttled,
)

BASE = "http://test-target"
LOGIN_OK = {"accessToken": "tok-1", "refreshToken": "ref-1", "expiresIn": 900}


def make_client(handler: Callable[[httpx.Request], httpx.Response]) -> CloudBalancerClient:
    return CloudBalancerClient(
        base_url=BASE,
        username="admin",
        password="admin",
        transport=httpx.MockTransport(handler),
    )


def test_login_stores_token_and_sends_bearer_header() -> None:
    seen_auth: list[str | None] = []

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        seen_auth.append(request.headers.get("Authorization"))
        return httpx.Response(200, json={"workerCount": 1})

    with make_client(handler) as client:
        client.get_scaling_status()  # lazy login on first request

    assert seen_auth == ["Bearer tok-1"]


def test_login_failure_raises_auth_error() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(401, json={"error": "bad credentials"})

    with make_client(handler) as client:
        with pytest.raises(AuthError):
            client.login()


def test_401_triggers_single_relogin_then_retry() -> None:
    calls = {"login": 0, "status": 0}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            calls["login"] += 1
            return httpx.Response(200, json={**LOGIN_OK, "accessToken": f"tok-{calls['login']}"})
        calls["status"] += 1
        # first authenticated call gets 401 (expired), retry succeeds
        if calls["status"] == 1:
            return httpx.Response(401)
        return httpx.Response(200, json={"workerCount": 2})

    with make_client(handler) as client:
        status = client.get_scaling_status()

    assert status.worker_count == 2
    assert calls["login"] == 2  # initial lazy login + re-login after 401


def test_persistent_401_raises_auth_error() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        return httpx.Response(401)

    with make_client(handler) as client:
        with pytest.raises(AuthError, match="re-login"):
            client.get_scaling_status()


def test_429_raises_throttled_with_retry_after() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        return httpx.Response(429, headers={"Retry-After": "60"})

    with make_client(handler) as client:
        with pytest.raises(Throttled) as exc_info:
            client.get_scaling_status()

    assert exc_info.value.retry_after == 60.0


def test_submit_task_serializes_descriptor_and_parses_envelope() -> None:
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        captured.update(json.loads(request.content))
        return httpx.Response(
            200,
            json={
                "id": "t-1",
                "state": "SUBMITTED",
                "submittedAt": "2026-06-07T12:00:00Z",
                "descriptor": {"executorType": "SHELL"},
                "executionHistory": [],
            },
        )

    descriptor = TaskDescriptor(
        executor_type="SHELL",
        execution_spec={"command": "echo hi"},
        priority="HIGH",
        execution_policy={"maxRetries": 1},
    )
    with make_client(handler) as client:
        envelope = client.submit_task(descriptor)

    assert captured == {
        "executorType": "SHELL",
        "executionSpec": {"command": "echo hi"},
        "priority": "HIGH",
        "executionPolicy": {"maxRetries": 1},
    }
    assert envelope.id == "t-1"
    assert envelope.state == "SUBMITTED"
    assert envelope.executor_type == "SHELL"


def test_descriptor_omits_optional_fields_when_unset() -> None:
    descriptor = TaskDescriptor(executor_type="SIMULATED", execution_spec={"durationMs": 2000})
    assert descriptor.to_json() == {
        "executorType": "SIMULATED",
        "executionSpec": {"durationMs": 2000},
    }


def test_list_tasks_pages_through_offsets() -> None:
    pages: list[dict] = []

    def make_task(i: int) -> dict:
        return {
            "id": f"t-{i}",
            "state": "QUEUED",
            "submittedAt": "2026-06-07T12:00:00Z",
            "descriptor": {"executorType": "SHELL"},
        }

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        params = dict(request.url.params)
        pages.append(params)
        offset = int(params["offset"])
        page = [make_task(i) for i in range(offset, min(offset + 2, 3))]
        return httpx.Response(
            200, json={"tasks": page, "total": 3, "offset": offset, "limit": 2}
        )

    with make_client(handler) as client:
        tasks = client.list_tasks(since="2026-06-07T11:59:55Z", limit=2)

    assert [t.id for t in tasks] == ["t-0", "t-1", "t-2"]
    assert pages[0]["since"] == "2026-06-07T11:59:55Z"
    assert [p["offset"] for p in pages] == ["0", "2"]


def test_cluster_metrics_returns_none_on_404() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        return httpx.Response(404)

    with make_client(handler) as client:
        assert client.get_cluster_metrics() is None


def test_cluster_metrics_parses_avg_cpu() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        return httpx.Response(200, json={"avgCpuPercent": 42.5, "workerCount": 3})

    with make_client(handler) as client:
        metrics = client.get_cluster_metrics()

    assert metrics is not None
    assert metrics.avg_cpu_percent == 42.5


def test_trigger_scaling_payload() -> None:
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(200, json=LOGIN_OK)
        captured.update(json.loads(request.content))
        return httpx.Response(200, json={})

    with make_client(handler) as client:
        client.trigger_scaling("SCALE_UP", 1, agent_id="agent-7")

    assert captured == {"action": "SCALE_UP", "count": 1, "agentId": "agent-7"}


def test_task_envelope_worker_attribution_from_history() -> None:
    envelope = TaskEnvelope.from_json(
        {
            "id": "t-9",
            "state": "RUNNING",
            "submittedAt": "2026-06-07T12:00:00Z",
            "descriptor": {"executorType": "PYTHON"},
            "executionHistory": [
                {"workerId": "w-1", "state": "ASSIGNED"},
                {"workerId": "w-2", "state": "RUNNING"},
            ],
        }
    )
    assert envelope.last_worker_id == "w-2"

    empty = TaskEnvelope.from_json({"id": "t-10", "descriptor": {}})
    assert empty.last_worker_id is None


def test_scaling_status_null_guards_last_decision() -> None:
    status = ScalingStatus.from_json(
        {"workerCount": 0, "activeWorkerCount": 0, "drainingWorkerCount": 0,
         "policy": None, "lastDecision": None, "cooldownRemainingSeconds": 0}
    )
    assert status.last_decision is None

    with_decision = ScalingStatus.from_json(
        {
            "workerCount": 2,
            "lastDecision": {
                "action": "SCALE_UP",
                "reason": "queue pressure",
                "triggerType": "QUEUE_PRESSURE",
                "previousWorkerCount": 1,
                "newWorkerCount": 2,
                "timestamp": "2026-06-07T12:01:00Z",
            },
        }
    )
    assert with_decision.last_decision is not None
    assert with_decision.last_decision.action == "SCALE_UP"
    assert with_decision.last_decision.new_worker_count == 2


def test_agent_parsing() -> None:
    agent = Agent.from_json(
        {
            "agentId": "agent-1",
            "hostname": "slave-1",
            "totalCpuCores": 8,
            "availableCpuCores": 6.5,
            "totalMemoryMB": 16384,
            "availableMemoryMB": 12000,
            "activeWorkerIds": ["w-1"],
            "supportedExecutors": ["SHELL", "PYTHON", "DOCKER", "SIMULATED"],
            "lastHeartbeat": "2026-06-07T12:00:30Z",
        }
    )
    assert agent.agent_id == "agent-1"
    assert agent.available_cpu_cores == 6.5
    assert "PYTHON" in agent.supported_executors
