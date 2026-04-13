# CloudBalancer Architecture Diagrams

## 1. Bird's Eye View — Services & Infrastructure

The simplest view: what runs, and what connects them.

```mermaid
graph LR
    Client([Client / API User])

    subgraph Infrastructure
        Kafka[(Apache Kafka)]
        DB[(PostgreSQL / TimescaleDB)]
    end

    subgraph CloudBalancer
        Dispatcher[Dispatcher<br/><small>REST API · Scheduler · Coordinator</small>]
        Worker1[Worker N]
        Worker2[Worker N+1]
        Metrics[Metrics Aggregator]
    end

    Client -->|REST / WebSocket| Dispatcher
    Client -->|REST| Metrics

    Dispatcher <-->|Kafka| Worker1
    Dispatcher <-->|Kafka| Worker2
    Worker1 -->|Kafka| Metrics
    Worker2 -->|Kafka| Metrics

    Dispatcher -->|JPA / Flyway| DB
    Metrics -->|JPA / Flyway| DB
```

---

## 2. Component Diagram — Modules & Kafka Topics

Shows the Kafka topic mesh that connects all services.

```mermaid
graph TB
    subgraph Dispatcher
        API[REST API]
        Scheduler[Task Scheduler]
        Registry[Worker Registry]
        Retry[Retry Scanner]
        WS[WebSocket Handler]
    end

    subgraph Worker["Worker (×N)"]
        Executor[Task Executor]
        Reporter[Metrics Reporter]
        Reg[Registration]
    end

    subgraph MetricsAgg[Metrics Aggregator]
        MetricsAPI[Metrics API]
        Aggregation[Aggregation Service]
    end

    Kafka{{Apache Kafka}}
    DB[(PostgreSQL)]
    TSDB[(TimescaleDB)]

    %% Registration flow
    Reg -->|workers.registration| Kafka
    Kafka -->|workers.registration| Registry

    %% Assignment flow
    Scheduler -->|tasks.assigned| Kafka
    Kafka -->|tasks.assigned| Executor

    %% Result flow
    Executor -->|tasks.results| Kafka
    Kafka -->|tasks.results| Scheduler

    %% Log streaming
    Executor -->|tasks.logs| Kafka
    Kafka -->|tasks.logs| WS

    %% Heartbeat & metrics
    Reporter -->|workers.heartbeat| Kafka
    Reporter -->|workers.metrics| Kafka
    Kafka -->|workers.heartbeat| Registry
    Kafka -->|workers.metrics| Aggregation

    %% Event bus
    Scheduler -->|tasks.events| Kafka
    Kafka -->|tasks.events| Aggregation

    %% Dead letter
    Retry -->|tasks.deadletter| Kafka

    %% Persistence
    Dispatcher -->|JPA| DB
    MetricsAgg -->|JPA| TSDB
```

---

## 3. Task Lifecycle — Sequence Diagram

The journey of a single task from submission to completion.

```mermaid
sequenceDiagram
    participant C as Client
    participant D as Dispatcher
    participant K as Kafka
    participant W as Worker
    participant DB as PostgreSQL

    C->>D: POST /api/tasks (TaskDescriptor)
    D->>DB: Save task (SUBMITTED → QUEUED)
    D-->>C: 200 OK (task ID)

    loop Every 1s (Scheduler)
        D->>DB: Fetch QUEUED tasks
        D->>D: Run scheduling pipeline<br/>(filters → strategy)
    end

    D->>DB: Update task (QUEUED → ASSIGNED)
    D->>K: tasks.assigned (keyed by workerId)
    K->>W: TaskAssignment

    W->>W: Resolve executor & run task
    W->>K: tasks.logs (streaming)
    K->>D: tasks.logs → WebSocket broadcast

    W->>D: POST /internal/tasks/{id}/artifacts
    W->>K: tasks.results (TaskResult)
    K->>D: TaskResult

    D->>DB: Update task (→ COMPLETED / FAILED)
    D->>K: tasks.events (TaskCompletedEvent)
```

---

## 4. Task State Machine

All possible states and transitions a task goes through.

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED
    SUBMITTED --> VALIDATED
    VALIDATED --> QUEUED

    QUEUED --> ASSIGNED : Scheduler matches worker
    ASSIGNED --> PROVISIONING
    PROVISIONING --> RUNNING

    RUNNING --> COMPLETED : Success
    RUNNING --> FAILED : Error
    RUNNING --> TIMED_OUT : Timeout

    FAILED --> QUEUED : Retry (within limits)
    TIMED_OUT --> QUEUED : Retry (within limits)

    FAILED --> DEAD_LETTERED : Max retries / poison pill
    TIMED_OUT --> DEAD_LETTERED : Max retries

    DEAD_LETTERED --> [*]
    COMPLETED --> [*]
```

---

## 5. Deployment View — Docker Compose

What actually runs when you `docker compose up`.

```mermaid
graph TB
    subgraph Docker Compose
        subgraph kafka_cluster[Kafka - KRaft Mode]
            Kafka[Kafka Broker<br/><small>:9092</small>]
        end

        subgraph database[Database]
            PG[(PostgreSQL 16<br/>+ TimescaleDB<br/><small>:5432</small>)]
        end

        subgraph services[Application Services]
            Disp[Dispatcher<br/><small>:8080</small>]
            W1[Worker 1<br/><small>headless</small>]
            W2[Worker 2<br/><small>headless</small>]
            MA[Metrics Aggregator<br/><small>:8081</small>]
        end
    end

    Disp --- Kafka
    W1 --- Kafka
    W2 --- Kafka
    MA --- Kafka

    Disp --- PG
    MA --- PG

    User([User]) -->|:8080| Disp
    User -->|:8081| MA

    W1 -.-|Docker socket| DockerD([Docker Daemon])
    W2 -.-|Docker socket| DockerD
```

---

## 6. Scheduling Pipeline Detail

How the dispatcher decides which worker gets a task.

```mermaid
graph LR
    Q[QUEUED Tasks<br/><small>sorted by priority</small>]

    subgraph Filters
        F1[Health Filter]
        F2[Executor Capability]
        F3[Resource Sufficiency]
        F4[Constraint Filter]
    end

    subgraph Strategy
        S{Active Strategy}
        RR[Round Robin]
        WRR[Weighted Round Robin]
        LC[Least Connections]
        RF[Resource Fit]
    end

    Q --> F1 --> F2 --> F3 --> F4
    F4 --> S
    S -.-> RR
    S -.-> WRR
    S -.-> LC
    S -.-> RF
    S --> Assign[Assign to<br/>selected worker]
```

---

## 7. Security & Auth Flow

JWT authentication across all services.

```mermaid
graph TB
    Client([Client])

    subgraph Dispatcher [:8080]
        Auth[AuthController<br/><small>/api/auth/login</small>]
        JWTFilter[JWT Filter]
        RateLimit[Rate Limit Filter<br/><small>Bucket4j per-user</small>]
        API[Protected API]
        WSAuth[WebSocket<br/>JWT Handshake]
    end

    subgraph MetricsAgg [:8081]
        JWTFilter2[JWT Filter]
        MAPI[Metrics API]
    end

    DB[(users / refresh_tokens)]

    Client -->|POST /api/auth/login| Auth
    Auth -->|BCrypt verify| DB
    Auth -->|JWT + Refresh Token| Client

    Client -->|Bearer JWT| JWTFilter
    JWTFilter --> RateLimit --> API

    Client -->|?token=JWT| WSAuth
    WSAuth -->|Log Stream| Client

    Client -->|Bearer JWT<br/><small>same secret</small>| JWTFilter2
    JWTFilter2 --> MAPI
```

---

## 8. Executor Types

The pluggable execution backends available on each worker.

```mermaid
graph TB
    subgraph Worker
        TES[TaskExecutionService]

        subgraph Executors
            SIM[Simulated Executor<br/><small>Sleep-based, for testing</small>]
            SHELL[Shell Executor<br/><small>Native OS process</small>]
            DOCKER[Docker Executor<br/><small>Container via Docker API</small>]
            PYTHON[Python Executor<br/><small>Python script runner</small>]
        end
    end

    TES -->|resolves by type| SIM
    TES -->|resolves by type| SHELL
    TES -->|resolves by type| DOCKER
    TES -->|resolves by type| PYTHON

    DOCKER -.->|Docker socket| DockerD([Docker Daemon])
    SHELL -.->|ProcessBuilder| OS([OS Process])
    PYTHON -.->|ProcessBuilder| Py([Python Runtime])
```
