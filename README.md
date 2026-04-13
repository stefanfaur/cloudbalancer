# CloudBalancer

A distributed task scheduling and execution platform built with Java 21, Spring Boot 4, Apache Kafka, and PostgreSQL/TimescaleDB. CloudBalancer dispatches tasks across a fleet of worker containers orchestrated by per-host agents, with pluggable execution backends (shell, Docker, Python, simulated), auto-scaling, fault tolerance, real-time log streaming, and a React dashboard for operational control.

## Architecture

```
                                        +--------------------+
                                        |  Web Dashboard     |
                                        |  (React / Vite)    |
                                        +---------+----------+
                                                  | HTTPS + WSS
                                                  v
Client / CLI  --REST/JWT-->   +---------------------+
                              |   Dispatcher :8080  |
                              |  REST, scheduling,  |
                              |  auto-scaling, WS   |
                              +----+----+------+----+
                                   |    |      |
                          JDBC     |    |Kafka |Kafka (REMOTE :29094, SASL)
                                   v    v      v
                       +-----------------+    +--------------------+
                       | PostgreSQL /    |    | Worker-Agent (per  |
                       | TimescaleDB     |    | host) -- Docker    |
                       +--------+--------+    | orchestrator       |
                                ^             +----------+---------+
                                |                        |
                                |                        | spawns
                                |                        v
                    +-----------+------------+    +------+-------+
                    | Metrics Aggregator     |<---+  Worker(s)   |
                    | :8081 (Kafka consumer) |Kafka| (executors) |
                    +------------------------+    +--------------+
```

- **Dispatcher** — REST API, task scheduling, worker/agent registry, auto-scaling, WebSocket log + event streaming, JWT auth.
- **Worker-Agent** — runs on each host, registers with the dispatcher using a pre-issued token, receives `StartWorkerCommand`/`StopWorkerCommand` over Kafka, and spawns/kills worker containers via the local Docker socket. Publishes heartbeats with per-host capacity.
- **Worker** — executes tasks via pluggable executors, publishes heartbeat/metrics/results over Kafka, supports drain-aware shutdown.
- **Metrics Aggregator** — consumes worker metrics from Kafka, persists time-series data in TimescaleDB, exposes metrics REST API.
- **Web Dashboard** — React 19 + TypeScript + Vite SPA for task submission, worker/agent monitoring, live metrics, scaling and chaos controls.
- **Common** — shared models, events, executor framework, agent protocol (no Spring dependency).

Full diagrams live in [docs/architecture-diagrams.md](docs/architecture-diagrams.md).

## Prerequisites

- **JDK 21** (auto-provisioned via Gradle toolchain)
- **Docker** + **Docker Compose** (required for Testcontainers and the dev stack)
- **Node.js 20+** and `npm` (only if running the web dashboard locally)
- No Gradle install needed — the `gradlew` wrapper handles it

## Quick Start

### Option A: Full Stack via Docker Compose

Build the backend JARs, then start everything:

```bash
./gradlew build -x test

cd docker
docker compose -f docker-compose.dev.yml up --build
```

The dev stack brings up:

| Service | Purpose | Host port |
|---------|---------|-----------|
| `kafka` | Apache Kafka 3.9 (KRaft, SASL REMOTE listener) | `9092`, `29094` |
| `postgres` | TimescaleDB on PostgreSQL 16 | `5432` |
| `nginx` | Reverse proxy fronting dispatcher + metrics | `80` |
| `dispatcher` | Control plane (REST + WebSocket) | via nginx `:80` |
| `metrics-aggregator` | Time-series metrics API | via nginx `:80` |
| `worker` | One seed worker (legacy host-process mode) | — |
| `worker-agent-1` | Host agent, capacity 8 CPU / 16 GB | — |
| `worker-agent-2` | Host agent, capacity 4 CPU / 8 GB | — |

The dispatcher runs with `SCALING_RUNTIME_MODE=AGENT`, so new workers are spawned as Docker containers through the worker-agents rather than as local processes.

### Option B: Infrastructure Only, Services via Gradle

Start Kafka + Postgres in Docker, run JVM services locally:

```bash
cd docker
docker compose -f docker-compose.dev.yml up kafka postgres
```

Then in separate terminals:

```bash
# Terminal 1: Dispatcher (:8080)
./gradlew :dispatcher:bootRun

# Terminal 2: Worker (default ID: worker-1)
./gradlew :worker:bootRun

# Terminal 3: Second worker with custom ID
WORKER_ID=worker-2 ./gradlew :worker:bootRun

# Terminal 4: Metrics Aggregator (:8081)
./gradlew :metrics-aggregator:bootRun

# Terminal 5 (optional): Worker-Agent (Docker orchestrator)
AGENT_ID=agent-1 ./gradlew :worker-agent:bootRun
```

### Option C: Web Dashboard Dev Server

The dashboard is a Vite SPA. From `web-dashboard/`:

```bash
cd web-dashboard
npm install
npm run dev      # http://localhost:5173

# Other scripts
npm run build    # production build to dist/
npm run test     # Vitest unit tests
npm run lint
```

Point it at the backend with `VITE_API_URL`, `VITE_METRICS_URL`, `VITE_WS_URL` (defaults target `http://localhost` / `ws://localhost`, i.e. the compose nginx).

### First Login

The dispatcher seeds a default admin on startup: `admin` / `admin`.

```bash
# Get a JWT access + refresh token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .accessToken)

# Use the token for authenticated requests
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tasks
```

## API Reference

### Dispatcher (`:8080`, proxied at `:80` via nginx)

#### Authentication (`/api/auth`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/login` | public | Login, returns access + refresh JWTs |
| POST | `/refresh` | public | Rotate refresh token, issue new access token |
| POST | `/logout` | authenticated | Revoke all refresh tokens for the caller |

#### Tasks (`/api/tasks`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `` | ADMIN, OPERATOR, API_CLIENT | Submit a new task |
| GET | `` | authenticated | List tasks with pagination + filters (`offset`, `limit`, `status`, `priority`, `executorType`, `workerId`, `since`) |
| GET | `/{id}` | authenticated | Get task by ID |
| GET | `/{id}/logs` | authenticated | Stored stdout / stderr for a finished task |
| GET | `/{id}/artifacts/{name}` | authenticated | Download a task artifact |
| POST | `/bulk/cancel` | ADMIN, OPERATOR | Cancel a batch of tasks |
| POST | `/bulk/retry` | ADMIN, OPERATOR | Retry a batch of failed tasks |
| POST | `/bulk/reprioritize` | ADMIN, OPERATOR | Update priority for many tasks |
| POST | `/internal/tasks/{taskId}/artifacts/{name}` | internal | Worker artifact upload (unauthenticated) |

#### Workers (`/api/workers`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `` | authenticated | List workers with health state and active task counts |

#### Agent Registration (`/api/agents`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | public (token) | Agent self-registration; dispatcher validates SHA-256 token hash, returns Kafka SASL credentials |

#### Admin — Agents (`/api/admin/agents`, ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| GET | `` | List live agents with capacity and host info |
| GET | `/{agentId}` | Agent detail |
| GET | `/{agentId}/workers` | Workers currently running on the agent |

#### Admin — Agent Tokens (`/api/admin/agent-tokens`, ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| POST | `` | Mint a new registration token (returned once, plaintext) |
| GET | `` | List token metadata (label, created, last-used, revoked) |
| POST | `/{id}/revoke` | Revoke a token |

#### Admin — Scheduling (`/api/admin`, ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/strategy` | Active scheduling strategy + weights |
| PUT | `/strategy` | Set strategy (`ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, `LEAST_CONNECTIONS`, `RESOURCE_FIT`, `CUSTOM`) |
| PUT | `/workers/{id}/tags` | Update the tag set for a worker (used by `TaskConstraints.requiredTags`) |

#### Auto-Scaling (`/api/scaling`, ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/status` | Current worker counts, policy, cooldown state |
| PUT | `/policy` | Update policy (`minWorkers`, `maxWorkers`, `cooldownPeriod`, `scaleUpStep`, `scaleDownStep`, `scaleDownDrainTime`) |
| POST | `/trigger` | Manually scale up/down by N |

#### Chaos (`/api/admin/chaos`, ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/kill-worker` | Terminate a (random or named) worker |
| POST | `/fail-task` | Force-fail a running task |
| POST | `/latency` | Inject artificial latency into a component for N seconds |

#### WebSocket
| Path | Description |
|------|-------------|
| `ws://localhost:8080/api/tasks/{id}/logs/stream?token=JWT` | Real-time stdout/stderr streaming for a task |
| `ws://localhost:8080/api/ws/events?token=JWT` | Dashboard event bus: task updates, worker state, scaling events, alerts, and initial snapshot |

### Metrics Aggregator (`:8081`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/metrics/workers` | authenticated | Latest metrics for all workers |
| GET | `/api/metrics/workers/{id}/history` | authenticated | Time-series metrics for one worker |
| GET | `/api/metrics/cluster` | authenticated | Cluster-wide aggregates |

## Submitting a Task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "hello-world",
    "executorType": "SHELL",
    "payload": {
      "command": "echo Hello from CloudBalancer && date"
    },
    "resourceProfile": {
      "cpuCores": 1,
      "memoryMB": 256,
      "diskMB": 100,
      "gpuRequired": false,
      "gpuMemoryMB": 0,
      "networkAccessRequired": false
    },
    "executionPolicy": {
      "timeoutSeconds": 30,
      "maxRetries": 2,
      "retryBackoffStrategy": "EXPONENTIAL"
    },
    "priority": "NORMAL"
  }'
```

**Executors:** `SIMULATED`, `SHELL`, `DOCKER`, `PYTHON`.
Shell commands run through an allow/deny list; Python runs inside a per-task venv with optional pip requirements and network-namespace isolation on Linux; Docker executes with hardened defaults (dropped capabilities, `no-new-privileges`, memory/CPU limits, optional read-only rootfs).

**Task states:** `SUBMITTED → VALIDATED → QUEUED → ASSIGNED → PROVISIONING → RUNNING → POST_PROCESSING → COMPLETED`, with terminal branches `CANCELLED`, `TIMED_OUT`, `FAILED`, `DEAD_LETTERED`. `FAILED` / `TIMED_OUT` re-queue until the `maxRetries` budget is exhausted.

## Scheduling & Scaling

- **Strategies:** `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, `LEAST_CONNECTIONS`, `RESOURCE_FIT`, `CUSTOM` (weighted blend of `WorkerScorer` components). Hot-swappable via `PUT /api/admin/strategy`.
- **Scaling triggers:** `REACTIVE` (CPU high/low windows), `QUEUE_PRESSURE` (queued-vs-active ratio), `MANUAL` (admin-initiated). Evaluated every 30 s by default.
- **Runtime modes:** `PROCESS` (legacy — spawn JVM workers locally) or `AGENT` (compose default — workers are Docker containers started by `worker-agent` instances per host). Set via `SCALING_RUNTIME_MODE`.
- **Worker constraints:** tasks may specify `requiredTags`, `whitelistedWorkers`, `blacklistedWorkers` via `TaskConstraints`.

## Fault Tolerance

- **Retries** with `FIXED`, `EXPONENTIAL`, or `EXPONENTIAL_WITH_JITTER` backoff per-task.
- **Dead-letter queue** — tasks exceeding `maxRetries` transition to `DEAD_LETTERED`.
- **Circuit breakers** (Resilience4j) around the Kafka producer.
- **Rate limiting** (Bucket4j) on authenticated endpoints.
- **Drain-aware shutdown** — workers accept `DrainCommand` with a grace period to finish in-flight tasks.
- **Kafka idempotency guards** on result and assignment listeners.

## Configuration

Key environment variables (defaults shown):

| Variable | Default | Scope | Description |
|----------|---------|-------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | all JVM services | Internal Kafka bootstrap |
| `KAFKA_EXTERNAL_BOOTSTRAP` | `localhost:29094` | dispatcher | REMOTE (SASL) bootstrap advertised to remote agents |
| `KAFKA_AGENT_USERNAME` / `KAFKA_AGENT_PASSWORD` | `cloudbalancer-agent` / `changeme` | dispatcher + remote agents | SASL_PLAINTEXT creds for REMOTE listener |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/cloudbalancer` | dispatcher, metrics | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `postgres` | dispatcher, metrics | DB creds |
| `SCALING_RUNTIME_MODE` | `AGENT` (compose) / `PROCESS` (local) | dispatcher | How scale-ups are materialised |
| `WORKER_ID` | `worker-1` | worker | Unique worker identifier |
| `DISPATCHER_URL` | `http://localhost:8080` | worker, agent | Dispatcher base URL |
| `AGENT_ID` | `agent-1` | worker-agent | Unique agent identifier |
| `AGENT_CPU_CORES` | `8` | worker-agent | Declared host capacity |
| `AGENT_MEMORY_MB` | `16384` | worker-agent | Declared host capacity |
| `AGENT_REGISTRATION_TOKEN` | — | worker-agent | Token from `POST /api/admin/agent-tokens` |
| `DOCKER_WORKER_IMAGE` | `docker-worker` | worker-agent | Image spawned for new workers |
| `DOCKER_NETWORK_NAME` | `docker_default` | worker-agent | Docker network joined by spawned workers |
| `VITE_API_URL` / `VITE_METRICS_URL` / `VITE_WS_URL` | `http://localhost` / `ws://localhost` | dashboard | Backend endpoints |

Scaling defaults live under `cloudbalancer.dispatcher.scaling.*` in `dispatcher/src/main/resources/application.yml`. Agent capacity and heartbeat cadence live under `cloudbalancer.agent.*` in `worker-agent/src/main/resources/application.yml`.

## Remote Agent Deployment

To attach an agent running on a different host:

1. Mint a registration token on the dispatcher:
   ```bash
   curl -X POST http://dispatcher-host/api/admin/agent-tokens \
     -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"label":"edge-host-1"}'
   ```
   The response contains the plaintext token — it is only shown once.

2. On the target host, copy `docker/docker-compose.agent.yml` and a `.env` file defining `AGENT_ID`, `AGENT_REGISTRATION_TOKEN`, `DISPATCHER_URL`, and `KAFKA_REMOTE_HOST`.

3. Start the agent:
   ```bash
   docker compose -f docker-compose.agent.yml up -d
   ```

The agent will POST to `/api/agents/register`, receive SASL Kafka credentials in response, and begin publishing heartbeats on `agents.heartbeat`. The dispatcher's `AgentRegistry` will schedule new workers onto it based on reported capacity.

## Project Structure

```
cloudbalancer/
  common/                  Shared models, events, agent protocol, executor framework
  dispatcher/              REST API, scheduling, auto-scaling, WebSocket, agent control plane
  worker/                  Task execution, heartbeat + metrics reporting, drain
  worker-agent/            Per-host Docker orchestrator, Kafka command listener
  metrics-aggregator/      Kafka consumers, TimescaleDB persistence, metrics REST API
  web-dashboard/           React 19 + Vite + shadcn/ui operational dashboard
  docker/                  docker-compose.dev.yml, docker-compose.agent.yml, nginx.conf
  docs/                    Architecture diagrams and design documents
```

## Running Tests

```bash
# Full backend build with tests (requires Docker for Testcontainers)
./gradlew build

# Per-module
./gradlew :common:test
./gradlew :dispatcher:test
./gradlew :worker:test
./gradlew :worker-agent:test
./gradlew :metrics-aggregator:test

# Dashboard
cd web-dashboard
npm run test          # Vitest unit + component tests
npx playwright test   # E2E (expects the dev stack running)
```

Backend tests use [Testcontainers](https://testcontainers.com/) to spin up Kafka and PostgreSQL automatically — Docker must be running.
