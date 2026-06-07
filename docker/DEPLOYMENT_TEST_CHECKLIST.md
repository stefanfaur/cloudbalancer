# Two-Machine Deployment Test Checklist

Use this checklist to verify the deployment is working end-to-end.

## Prerequisites

- [ ] Both machines on the same Tailscale network
- [ ] Docker + Compose v2 on both machines
- [ ] Java 21 installed
- [ ] Repo cloned on both machines

## Master Setup

- [ ] `.env` created with MASTER_HOST, passwords, JWT_SECRET
- [ ] `./gradlew :dispatcher:bootJar :metrics-aggregator:bootJar` completed
- [ ] `docker compose -f docker-compose.master.yml up -d --build` succeeded
- [ ] Wait 15 seconds for services to stabilize
- [ ] Dashboard loads at http://<MASTER_HOST>/
- [ ] Default admin/admin credentials work
- [ ] **Password changed in Admin settings**
- [ ] Agent token generated (Admin > Agent Tokens > New Token)

## Slave Setup

- [ ] `.env` created with MASTER_HOST (same as master), AGENT_ID, credentials
- [ ] AGENT_REGISTRATION_TOKEN set to the token from dashboard
- [ ] AGENT_CPU_CORES >= 4 and AGENT_MEMORY_MB >= 8192 — must fit the
      dispatcher's default worker profile (`default-worker-cpu-cores: 4`,
      `default-worker-memory-mb: 8192`) or scale-up is rejected with
      "No agent with sufficient capacity"
- [ ] `./gradlew :worker-agent:bootJar :worker:bootJar` completed
- [ ] `docker compose -f docker-compose.slave.yml --profile build build` succeeded
- [ ] `docker images | grep cloudbalancer-worker` shows the image
- [ ] `docker compose -f docker-compose.slave.yml up -d agent` started
- [ ] `docker logs cloudbalancer-agent-remote` shows "Registration successful"

## Verification

- [ ] Dashboard > Agents shows the new agent with correct ID and capacity
- [ ] With zero workers running, trigger a manual scale-up first
      (Dashboard > Scaling, or `POST /api/scaling/trigger` with
      `{"action":"SCALE_UP","count":1}`) — the auto-scaler cannot
      cold-start from an empty metrics window
- [ ] Worker container appeared on slave (`docker ps` on slave shows cloudbalancer-worker-*)
- [ ] Dashboard > Create Task > SHELL, enter `echo "test"`
- [ ] Submit task
- [ ] Task status transitions to RUNNING then COMPLETED
- [ ] Task logs are downloadable (stdout shows the echoed text)
- [ ] No SASL/Kafka errors in worker logs

## Cleanup

- [ ] `docker compose -f docker-compose.master.yml down -v` on master
- [ ] `docker compose -f docker-compose.slave.yml down` on slave
- [ ] Verify both machines have no dangling cloudbalancer containers

---

**Timestamp:** 2026-06-07
