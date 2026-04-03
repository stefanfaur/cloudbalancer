# CloudBalancer

A distributed task scheduling and execution platform built with Java 21, Spring Boot, Apache Kafka, and PostgreSQL/TimescaleDB. CloudBalancer dispatches tasks to a pool of workers that support multiple execution backends (shell commands, Docker containers, Python scripts), with automatic scaling, fault tolerance, real-time log streaming, and comprehensive metrics.

## Architecture

```
Client --> Dispatcher (:8080) <--Kafka--> Worker(s) --> Executor (Shell/Docker/Python)
              |                              |
              v                              v
           PostgreSQL                  Kafka --> Metrics Aggregator (:8081)
                                                       |
                                                       v
                                                  TimescaleDB
```

- **Dispatcher** -- REST API, task scheduling, worker registry, auto-scaling, WebSocket log streaming
- **Worker** -- task execution via pluggable executors, heartbeat/metrics reporting, drain-aware shutdown
- **Metrics Aggregator** -- consumes worker metrics from Kafka, stores time-series data, provides metrics API
- **Common** -- shared models, events, executor framework (no Spring dependency)

For detailed diagrams see [docs/architecture-diagrams.md](docs/architecture-diagrams.md).

## Prerequisites

- **JDK 21** (auto-provisioned via Gradle toolchain if missing)
- **Docker** and **Docker Compose**
- No Gradle installation needed -- the included `gradlew` wrapper handles it

## Quick Start

### Option A: Full Stack via Docker Compose

Build the JARs, then start everything:

```bash
./gradlew build -x test

cd docker
docker compose -f docker-compose.dev.yml up --build
```

This starts Kafka, PostgreSQL/TimescaleDB, dispatcher (:8080), one worker, and the metrics aggregator (:8081).

### Option B: Local Development (services via Gradle)

Start only the infrastructure in Docker:

```bash
cd docker
docker compose -f docker-compose.dev.yml up kafka postgres
```

Then run the services individually (each in its own terminal):

```bash
# Terminal 1: Dispatcher
./gradlew :dispatcher:bootRun

# Terminal 2: Worker (default ID: worker-1)
./gradlew :worker:bootRun

# Terminal 3: Worker with custom ID
WORKER_ID=worker-2 ./gradlew :worker:bootRun

# Terminal 4: Metrics Aggregator
./gradlew :metrics-aggregator:bootRun
```

### First Login

On startup, the dispatcher seeds a default admin user: `admin` / `admin`.

```bash
# Get a JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .accessToken)

# Use the token for authenticated requests
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tasks
```

## API Reference

### Dispatcher (`:8080`)

#### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Login, returns JWT access + refresh tokens |
| POST | `/api/auth/refresh` | Refresh an expired access token |

#### Tasks
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/tasks` | ADMIN, OPERATOR, API_CLIENT | Submit a new task |
| GET | `/api/tasks` | Yes | List all tasks |
| GET | `/api/tasks/{id}` | Yes | Get task by ID |
| GET | `/api/tasks/{id}/logs` | Yes | Get stored logs for a task |
| GET | `/api/tasks/{id}/artifacts/{name}` | Yes | Download a task artifact |
| POST | `/internal/tasks/{taskId}/artifacts/{name}` | No (internal) | Worker artifact upload |

#### Auto-Scaling (ADMIN only)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/scaling/status` | Current scaling status, worker counts, cooldown |
| PUT | `/api/scaling/policy` | Update scaling policy (min/max workers, cooldown) |
| POST | `/api/scaling/trigger` | Manually trigger scale up/down |

#### Admin (ADMIN only)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/strategy` | Get active scheduling strategy |
| PUT | `/api/admin/strategy` | Set scheduling strategy (ROUND_ROBIN, LEAST_CONNECTIONS, RESOURCE_FIT, WEIGHTED_ROUND_ROBIN) |
| POST | `/api/admin/chaos/kill-worker` | Chaos: simulate worker death |
| POST | `/api/admin/chaos/fail-task` | Chaos: fail a running task |
| POST | `/api/admin/chaos/latency` | Chaos: inject latency |

#### WebSocket
| Path | Description |
|------|-------------|
| `ws://localhost:8080/api/tasks/{id}/logs/stream?token=JWT` | Real-time log streaming |

### Metrics Aggregator (`:8081`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/metrics/workers` | Yes | Latest metrics for all workers |
| GET | `/api/metrics/workers/{id}/history` | Yes | Time-series metrics for a worker |
| GET | `/api/metrics/cluster` | Yes | Cluster-wide aggregated metrics |

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

Executor types: `SIMULATED`, `SHELL`, `DOCKER`, `PYTHON`.

## Configuration

Key environment variables (with defaults):

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/cloudbalancer` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `WORKER_ID` | `worker-1` | Unique worker identifier |
| `DISPATCHER_URL` | `http://localhost:8080` | Dispatcher base URL (for artifact uploads) |

Scaling configuration is under `cloudbalancer.dispatcher.scaling.*` in `dispatcher/src/main/resources/application.yml`.

## Project Structure

```
cloudbalancer/
  common/                  Shared models, events, executor framework
  dispatcher/              REST API, scheduling, auto-scaling, WebSocket
  worker/                  Task execution, metrics/heartbeat reporting
  metrics-aggregator/      Time-series metrics storage and API
  docker/                  Docker Compose dev stack
  docs/                    Architecture diagrams
```

## Running Tests

```bash
# Full build with tests (requires Docker for Testcontainers)
./gradlew build

# Single module
./gradlew :dispatcher:test
./gradlew :worker:test
./gradlew :common:test
./gradlew :metrics-aggregator:test
```

Tests use [Testcontainers](https://testcontainers.com/) to spin up Kafka and PostgreSQL automatically. Docker must be running.
