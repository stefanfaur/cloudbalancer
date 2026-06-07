"""CloudBalancerClient — auth, task submission, and polling over REST.

All calls go through a single httpx.Client against the nginx-fronted
origin. Auth is JWT bearer; on 401 the client re-logins once and retries.
429 responses surface as Throttled (with the Retry-After delay) so the
monitor can pause polling instead of failing the run.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

import httpx


class ClientError(Exception):
    """Base class for client failures."""


class AuthError(ClientError):
    """Login failed, or re-login after a 401 failed again immediately."""


class Throttled(ClientError):
    """Rate-limited (429). Caller should pause for retry_after seconds."""

    def __init__(self, retry_after: float) -> None:
        super().__init__(f"rate limited; retry after {retry_after:.0f}s")
        self.retry_after = retry_after


@dataclass
class TaskDescriptor:
    """Submission payload for POST /api/tasks."""

    executor_type: str
    execution_spec: dict[str, Any]
    priority: str | None = None
    execution_policy: dict[str, Any] | None = None

    def to_json(self) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "executorType": self.executor_type,
            "executionSpec": self.execution_spec,
        }
        if self.priority is not None:
            payload["priority"] = self.priority
        if self.execution_policy is not None:
            payload["executionPolicy"] = self.execution_policy
        return payload


@dataclass
class TaskEnvelope:
    """Server-side task representation (subset we consume)."""

    id: str
    state: str
    submitted_at: str
    executor_type: str
    execution_history: list[dict[str, Any]] = field(default_factory=list)
    raw: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> "TaskEnvelope":
        descriptor = data.get("descriptor") or {}
        return cls(
            id=str(data.get("id", "")),
            state=str(data.get("state", "UNKNOWN")),
            submitted_at=str(data.get("submittedAt", "")),
            executor_type=str(descriptor.get("executorType", "UNKNOWN")),
            execution_history=data.get("executionHistory") or [],
            raw=data,
        )

    @property
    def last_worker_id(self) -> str | None:
        """Worker attribution: last executionHistory entry's workerId."""
        if not self.execution_history:
            return None
        return self.execution_history[-1].get("workerId")


@dataclass
class Agent:
    """Registered worker agent (GET /api/admin/agents)."""

    agent_id: str
    hostname: str
    total_cpu_cores: float
    available_cpu_cores: float
    total_memory_mb: float
    available_memory_mb: float
    active_worker_ids: list[str]
    supported_executors: list[str]
    last_heartbeat: str

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> "Agent":
        return cls(
            agent_id=str(data.get("agentId", "")),
            hostname=str(data.get("hostname", "")),
            total_cpu_cores=float(data.get("totalCpuCores") or 0),
            available_cpu_cores=float(data.get("availableCpuCores") or 0),
            total_memory_mb=float(data.get("totalMemoryMB") or 0),
            available_memory_mb=float(data.get("availableMemoryMB") or 0),
            active_worker_ids=data.get("activeWorkerIds") or [],
            supported_executors=data.get("supportedExecutors") or [],
            last_heartbeat=str(data.get("lastHeartbeat", "")),
        )


@dataclass
class ScalingDecision:
    """Nested lastDecision object from GET /api/scaling/status."""

    action: str
    reason: str
    trigger_type: str
    previous_worker_count: int
    new_worker_count: int
    timestamp: str

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> "ScalingDecision":
        return cls(
            action=str(data.get("action", "")),
            reason=str(data.get("reason", "")),
            trigger_type=str(data.get("triggerType", "")),
            previous_worker_count=int(data.get("previousWorkerCount") or 0),
            new_worker_count=int(data.get("newWorkerCount") or 0),
            timestamp=str(data.get("timestamp", "")),
        )


@dataclass
class ScalingStatus:
    """GET /api/scaling/status response."""

    worker_count: int
    active_worker_count: int
    draining_worker_count: int
    policy: dict[str, Any] | None
    last_decision: ScalingDecision | None
    cooldown_remaining_seconds: float

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> "ScalingStatus":
        decision = data.get("lastDecision")
        return cls(
            worker_count=int(data.get("workerCount") or 0),
            active_worker_count=int(data.get("activeWorkerCount") or 0),
            draining_worker_count=int(data.get("drainingWorkerCount") or 0),
            policy=data.get("policy"),
            last_decision=ScalingDecision.from_json(decision) if decision else None,
            cooldown_remaining_seconds=float(data.get("cooldownRemainingSeconds") or 0),
        )


@dataclass
class ClusterMetrics:
    """GET /api/metrics/cluster (metrics-aggregator) — best-effort."""

    raw: dict[str, Any]

    @property
    def avg_cpu_percent(self) -> float | None:
        value = self.raw.get("avgCpuPercent")
        return float(value) if value is not None else None

    @property
    def total_heap_used_mb(self) -> float | None:
        value = self.raw.get("totalHeapUsedMB")
        return float(value) if value is not None else None

    @property
    def throughput_per_minute(self) -> float | None:
        value = self.raw.get("throughputPerMinute")
        return float(value) if value is not None else None


class CloudBalancerClient:
    """Synchronous REST client for one CloudBalancer deployment."""

    def __init__(
        self,
        base_url: str,
        username: str,
        password: str,
        timeout: float = 10.0,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self._access_token: str | None = None
        self._http = httpx.Client(
            base_url=self.base_url,
            timeout=timeout,
            transport=transport,
            # talk to the deployment origin directly; never route through
            # HTTP(S)_PROXY/ALL_PROXY from the caller's environment
            trust_env=False,
        )

    def close(self) -> None:
        self._http.close()

    def __enter__(self) -> "CloudBalancerClient":
        return self

    def __exit__(self, *exc_info: object) -> None:
        self.close()

    # -- auth ------------------------------------------------------------

    def login(self) -> None:
        """POST /api/auth/login; stores the access token for later calls."""
        response = self._http.post(
            "/api/auth/login",
            json={"username": self.username, "password": self.password},
        )
        if response.status_code != 200:
            raise AuthError(
                f"login failed for '{self.username}': HTTP {response.status_code}"
            )
        body = response.json()
        token = body.get("accessToken")
        if not token:
            raise AuthError("login response missing accessToken")
        self._access_token = token

    # -- core request plumbing --------------------------------------------

    def _request(
        self,
        method: str,
        path: str,
        json_body: dict[str, Any] | None = None,
        params: dict[str, Any] | None = None,
        _retried_auth: bool = False,
    ) -> httpx.Response:
        if self._access_token is None:
            self.login()
        headers = {"Authorization": f"Bearer {self._access_token}"}
        response = self._http.request(
            method, path, json=json_body, params=params, headers=headers
        )
        if response.status_code == 401:
            if _retried_auth:
                raise AuthError("401 persisted immediately after re-login")
            self.login()
            return self._request(
                method, path, json_body=json_body, params=params, _retried_auth=True
            )
        if response.status_code == 429:
            retry_after = response.headers.get("Retry-After")
            try:
                delay = float(retry_after) if retry_after else 60.0
            except ValueError:
                delay = 60.0
            raise Throttled(delay)
        response.raise_for_status()
        return response

    # -- tasks -------------------------------------------------------------

    def submit_task(self, descriptor: TaskDescriptor) -> TaskEnvelope:
        response = self._request("POST", "/api/tasks", json_body=descriptor.to_json())
        return TaskEnvelope.from_json(response.json())

    def list_tasks(self, since: str, limit: int = 500) -> list[TaskEnvelope]:
        """GET /api/tasks?since=...&limit=...; pages with offset until done.

        `since` must be the server-assigned submittedAt floor (minus the
        safety margin) — never the client clock.
        """
        envelopes: list[TaskEnvelope] = []
        offset = 0
        while True:
            response = self._request(
                "GET",
                "/api/tasks",
                params={"since": since, "limit": limit, "offset": offset},
            )
            body = response.json()
            page = body.get("tasks") or []
            envelopes.extend(TaskEnvelope.from_json(item) for item in page)
            total = int(body.get("total") or len(envelopes))
            offset += len(page)
            if offset >= total or not page:
                break
        return envelopes

    def get_task_logs(self, task_id: str) -> dict[str, str]:
        """GET /api/tasks/{id}/logs -> {stdout, stderr}. Report-time only."""
        response = self._request("GET", f"/api/tasks/{task_id}/logs")
        body = response.json()
        return {
            "stdout": body.get("stdout") or "",
            "stderr": body.get("stderr") or "",
        }

    # -- cluster ------------------------------------------------------------

    def get_agents(self) -> list[Agent]:
        response = self._request("GET", "/api/admin/agents")
        return [Agent.from_json(item) for item in response.json()]

    def get_scaling_status(self) -> ScalingStatus:
        response = self._request("GET", "/api/scaling/status")
        return ScalingStatus.from_json(response.json())

    def get_cluster_metrics(self) -> ClusterMetrics | None:
        """Best-effort: returns None on 404 or connection failure."""
        try:
            response = self._request("GET", "/api/metrics/cluster")
        except Throttled:
            raise
        except (httpx.HTTPStatusError, httpx.TransportError):
            return None
        return ClusterMetrics(raw=response.json())

    def trigger_scaling(self, action: str, count: int, agent_id: str | None = None) -> None:
        payload: dict[str, Any] = {"action": action, "count": count}
        if agent_id is not None:
            payload["agentId"] = agent_id
        self._request("POST", "/api/scaling/trigger", json_body=payload)
