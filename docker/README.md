# Two-Machine CloudBalancer Deployment

This directory contains Docker Compose configurations for deploying CloudBalancer across two machines:
- **Master**: Control plane with Kafka, PostgreSQL, dispatcher, metrics-aggregator, and dashboard
- **Slave**: Remote agent that spawns workers and connects back to the master

## Compose Files

| File | Purpose | Usage |
|------|---------|-------|
| `docker-compose.dev.yml` | Full stack (local dev, all services on one machine) | `docker compose -f docker-compose.dev.yml up -d` |
| `docker-compose.agent.yml` | Registry-based agent deployment (existing flow) | `docker compose -f docker-compose.agent.yml up -d` |
| `docker-compose.master.yml` | Control plane only (Kafka, postgres, dispatcher, metrics, dashboard) | `docker compose -f docker-compose.master.yml up -d --build` |
| `docker-compose.slave.yml` | Remote agent + worker image builder | `docker compose -f docker-compose.slave.yml up -d agent` |

## Topology

```
┌─────────────────────────────────────────────────────────────────┐
│ Master Machine (Tailscale IP: e.g., 100.x.y.1)                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Docker                                                       │  │
│  │  ├─ kafka:29092 (internal), :29094 (SASL_PLAINTEXT remote) │  │
│  │  ├─ postgres:5432 (localhost only)                         │  │
│  │  ├─ dispatcher:8080 → public :8080 (registration + API)    │  │
│  │  ├─ metrics-aggregator:8081 (compose network only)         │  │
│  │  └─ nginx:80 (dashboard + API proxy)                       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌─ Port 80: dashboard (SPA relative paths)                     │
│  ├─ Port 8080: dispatcher (agent registration + artifact upload) │
│  └─ Port 29094: Kafka REMOTE (SASL_PLAINTEXT)                   │
└────────────────────────────────────────────────────────────────┘
                           ▲ Tailscale overlay
                           │ TCP/IP
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Slave Machine (Tailscale IP: e.g., 100.x.y.2)                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Docker (local compose)                                       │  │
│  │  ├─ agent container (registered mode)                       │  │
│  │  │   └─ Spawns worker containers on demand                  │  │
│  │  └─ cloudbalancer-worker image (local build)               │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Workers connect to:                                             │
│  ├─ Kafka: 100.x.y.1:29094 (SASL_PLAINTEXT)                     │
│  └─ Dispatcher: 100.x.y.1:8080 (artifact upload)                │
└────────────────────────────────────────────────────────────────┘
```

## Prerequisites (Both Machines)

- **Docker + Compose v2** (`docker compose version` shows v2.x or later)
- **Java 21** (for Gradle builds)
- **Tailscale** connected (both machines on the same tailnet)
- **Repo cloned** on both machines: `git clone <repo> && cd <repo>`
- **Git permissions** set up (SSH keys or credentials)

Verify prerequisites:

```bash
docker --version && docker compose version
java --version | grep 21
tailscale status | grep "100\."  # Shows your Tailscale IP
```

## Master Setup

1. **Copy and populate env file:**

   ```bash
   cd docker
   cp master.env.example .env
   ```

   Edit `.env` and set:

   ```
   MASTER_HOST=<your-tailscale-ip>  # e.g., 100.x.y.1
   KAFKA_ADMIN_PASSWORD=<strong-password>
   KAFKA_AGENT_PASSWORD=<strong-password>
   JWT_SECRET=$(openssl rand -base64 48)
   ```

2. **Build and start services:**

   ```bash
   ./gradlew :dispatcher:bootJar :metrics-aggregator:bootJar
   docker compose -f docker-compose.master.yml up -d --build
   sleep 10
   ```

   Check logs:

   ```bash
   docker logs cloudbalancer-dispatcher-master | tail -20
   docker logs cloudbalancer-dashboard-master | tail -10
   ```

3. **Log in to the dashboard:**

   Open http://<MASTER_HOST>/ in your browser. Default credentials:
   - Username: `admin`
   - Password: `admin`

   **⚠️ Change the password immediately.**

4. **Generate an agent registration token:**

   In the dashboard, go to **Admin > Agent Tokens > New Token** and copy the token (format: `cb_at_...`). You'll paste this into the slave's `.env`.

## Slave Setup

1. **Copy and populate env file:**

   ```bash
   cd docker
   cp slave.env.example .env
   ```

   Edit `.env` and set:

   ```
   MASTER_HOST=<master-tailscale-ip>  # Same as the master's MASTER_HOST
   AGENT_ID=agent-remote-1
   AGENT_CPU_CORES=4                  # Adjust to your machine
   AGENT_MEMORY_MB=8192               # Adjust to your machine
   AGENT_REGISTRATION_TOKEN=cb_at_... # From dashboard Admin > Agent Tokens
   ```

2. **Build the worker image (locally):**

   ```bash
   ./gradlew :worker-agent:bootJar :worker:bootJar
   docker compose -f docker-compose.slave.yml --profile build build
   ```

   This builds the `cloudbalancer-worker` image locally and caches it.

3. **Start the agent:**

   ```bash
   docker compose -f docker-compose.slave.yml up -d agent
   sleep 5
   docker logs cloudbalancer-agent-remote | tail -20
   ```

   **Expected:** You should see a log message like:

   ```
   Registration successful — Kafka bootstrap: 100.x.y.1:29094, username: cloudbalancer-agent
   ```

4. **Verify agent visibility in the dashboard:**

   In the master's dashboard, go to **Agents**. The new agent should appear with its ID, status, and capacity.

## Verification

### End-to-End Test

1. **In the dashboard on the master, go to Tasks → Create Task.**

2. **Create a SHELL task:**

   ```
   Command: echo "Hello from remote worker"
   ```

3. **Submit the task.** The system will:
   - Route the task to the available agent (on the slave)
   - Agent spawns a `cloudbalancer-worker` container
   - Worker connects to the master's Kafka (SASL_PLAINTEXT, 100.x.y.1:29094)
   - Worker executes the shell command
   - Worker uploads logs and artifacts to dispatcher:8080/internal/tasks/...
   - Task status updates in the dashboard

4. **Check the result:**
   - Task status → COMPLETED
   - Logs available for download
   - Artifacts viewable in the dashboard

### Agent Registration Failure Troubleshooting

If the agent logs show a 401 or can't reach the master:

```bash
# On the slave:
docker logs cloudbalancer-agent-remote | grep -i "registration\|error\|unauthorized"
```

See the **Troubleshooting** section below.

## Adding More Slaves

To add additional remote agents:

1. Clone the repo on a new machine (same network, Tailscale connected).
2. Copy the repo, `cd docker && cp slave.env.example .env`.
3. Set a unique `AGENT_ID` (e.g., `agent-remote-2`, `agent-remote-3`).
4. Use the **same `MASTER_HOST`**, **same `AGENT_REGISTRATION_TOKEN`** (or generate a new one in the dashboard).
5. Run:

   ```bash
   ./gradlew :worker-agent:bootJar :worker:bootJar
   docker compose -f docker-compose.slave.yml --profile build build
   docker compose -f docker-compose.slave.yml up -d agent
   ```

Each agent operates independently; tasks are routed by the dispatcher's `AgentRegistry.selectBestHost` (capacity-aware).

## Security Notes

- **Default dashboard credentials (`admin`/`admin`) must be changed** immediately after first login.
- **JWT_SECRET must be set** to a random, base64-encoded value (at least 32 bytes). The committed default in `application.yml` is public in git history — **do not rely on it**.
- **Kafka credentials (`KAFKA_ADMIN_PASSWORD`, `KAFKA_AGENT_PASSWORD`) must be strong.** Use `openssl rand -base64 16` or similar.
- **Tailscale encryption** provides transport security; plain HTTP and SASL_PLAINTEXT are safe over the overlay.
- **Relative dashboard URLs (same-origin only)** — Do not set `VITE_API_URL=http://100.x.y.1` in the build unless you also override `cloudbalancer.cors.allowed-origins` in the dispatcher. The dashboard is intentionally locked to same-origin for simplicity.
- **Kafka auto-creation** must remain enabled (`auto.create.topics.enable=true` is default). Topics are created by dispatcher/metrics on the internal listener and consumed by remote clients on the SASL listener.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Agent log: `401 Unauthorized` on registration | Token revoked or mistyped | Regenerate token in dashboard Admin > Agent Tokens |
| Agent exits after 10 retries | Master unreachable | Check `MASTER_HOST` is correct, port 8080 is published on master (`docker ps`), Tailscale is up on both machines (`tailscale status`) |
| Worker container starts then exits immediately | Kafka SASL authentication failure | Check `KAFKA_AGENT_PASSWORD` matches on master, or master advertises wrong Kafka address (should be `MASTER_HOST:29094`) |
| Workers can't reach master at all | Tailscale down or firewall blocking Docker bridge | On slave host, run `tailscale status` (should show connected); check Docker bridge can route to `tailscale0` (default on Linux) |
| Agent log: `Image not found: cloudbalancer-worker` | Worker image not built on slave | Rerun `docker compose -f docker-compose.slave.yml --profile build build` |
| Dashboard loads but API calls fail | CORS issue or relative URL misconfiguration | Verify dashboard was built with `VITE_API_URL=""` (empty string); check nginx.master.conf is in use; verify dispatcher is accessible at the host you're browsing from |
| Dashboard can't connect WebSocket | Wrong WS_URL fallback | Verify you merged the use-websocket.ts fix (window.location fallback) and rebuilt the dashboard |

### Manual Debugging

**On the master:**

```bash
# Check Kafka broker is up (EXTERNAL plaintext listener, inside the container)
docker exec cloudbalancer-kafka-master /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 | head -5

# Check dispatcher registration endpoint
curl -X POST http://localhost:8080/api/agents/register \
  -H "Content-Type: application/json" \
  -d '{"agentId":"test","token":"invalid"}'
# Expected: 401 Unauthorized (or similar auth error)

# Check metrics-aggregator (port 8081 is not published on the host;
# go through the nginx proxy — a 401 without a JWT still proves it's alive)
curl -i http://localhost/api/metrics/cluster
```

**On the slave:**

```bash
# Check agent container logs
docker logs cloudbalancer-agent-remote -f

# Check worker image exists
docker images | grep cloudbalancer-worker

# Check Docker socket is available
ls -la /var/run/docker.sock
```

## Known Limitations

- No Kubernetes support; orchestration is manual (one agent per machine).
- Per-agent Kafka credentials not yet supported (all agents use the same `cloudbalancer-agent` user).
- CORS locked to same-origin (no cross-origin dashboard hosting without code changes).
- TLS not in scope (Tailscale provides transport encryption).

## References

- Design document: `thoughts/shared/plans/cloudbalancer/2026-06-07-two-machine-deployment-design.md`
- Dispatcher registration code: `dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AgentRegistrationController.java`
- Agent registration client: `worker-agent/src/main/java/com/cloudbalancer/agent/registration/AgentRegistrationClient.java`
- Worker SASL configuration: `worker/src/main/resources/application.yml`

---

**Last updated:** 2026-06-07
**Maintained by:** CloudBalancer team
