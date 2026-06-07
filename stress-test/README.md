# CloudBalancer Stress Test CLI

Stress-tests a running CloudBalancer deployment. The CLI submits realistic workloads across all four implemented executor types (SHELL, PYTHON, DOCKER, SIMULATED), renders live progress in a Rich terminal dashboard, and prints a final report with latency percentiles and an exit code. Use it as an operational demo or as a deployment verification tool.

## Quick start

```bash
cd stress-test
uv sync

# quickest check: one task per executor type
uv run cloudbalancer-stress smoke

# provoke a QUEUE_PRESSURE scale-up
uv run cloudbalancer-stress burst

# target a remote deployment with a reproducible plan
uv run cloudbalancer-stress chaos --url http://100.x.y.1 --seed 42
```

The run exits 0 when every task settles and any failures match the scenario's expectations; it exits 1 on timeout, unexpected failures, or zero submissions — so it slots directly into CI or deployment checks.

## Scenarios

| Scenario | Shape | Exercises |
|----------|-------|-----------|
| `smoke` | 1 task per executor type at t=0 | routing of all executors; quick |
| `burst` | 30 tasks at t=0, mixed 30/30/20/20 shell/python/docker/sim | QUEUE_PRESSURE scale-up |
| `ramp` | rate climbs 1/15s → 1/8s → 1/3s over ~6 min | repeated scaling evaluations |
| `steady` | 1 task per 6s for ~5 min | sustained routing; scale-down afterwards |
| `chaos` | burst mix + flaky (40% fail), failing, and timing-out tasks | retries, FAILED→QUEUED recycling, TIMED_OUT, DEAD_LETTERED |

Each scenario renders from a seeded `random.Random`, so the same seed always produces the same plan. About 15% of tasks carry HIGH priority to make queue-jumping visible. Only `chaos` sets `allow_failures`; its dead-lettered tasks still exit 0.

## Flags

| Flag | Default | Env override | Purpose |
|------|---------|--------------|---------|
| `--url` | `http://localhost` | `CB_STRESS_URL` | deployment origin |
| `--username` | `admin` | `CB_STRESS_USERNAME` | login user |
| `--password` | `admin` | `CB_STRESS_PASSWORD` | login password |
| `--poll-interval` | `2.0` | — | seconds between status polls |
| `--run-timeout` | `900` | — | abort the run after this many seconds |
| `--seed` | `1234` | — | scenario RNG seed |
| `--no-bootstrap` | off | — | never trigger a cold-start SCALE_UP |

**Point `--url` at the nginx-fronted origin (port 80), not the dispatcher's `:8080`.** The separate metrics-aggregator serves `/api/metrics/*`, and only nginx routes both services under one origin. A `:8080`-pointed run still works — the CLI treats cluster metrics as best-effort and drops the header stat instead of failing.

## Cold start (bootstrap)

The auto-scaler cannot react from an empty metrics window, so the CLI bootstraps cold deployments itself:

1. If no agents are alive **and** the worker count is zero, the run aborts: start (or restart) a slave agent first.
2. If agents exist but the worker count is zero, the CLI triggers one manual `SCALE_UP` on the agent with the most available CPU.
3. `--no-bootstrap` skips step 2 (observe-only).

At run start the CLI also compares the scenario's executor types against the union of `supportedExecutors` across registered agents and warns about any type with no capable host.

## Dashboard

The live dashboard shows:

- **header** — scenario, target URL, elapsed time, submitted/planned counts, worker count, cluster CPU %
- **state counts** — every task state with semantic colors; unknown server states render verbatim
- **per-executor progress** — settled/planned bars for SHELL, PYTHON, DOCKER, SIMULATED
- **agents table** — CPU and memory (available/total), active workers, supported executors
- **event log** — last 5 scaling decisions (`QUEUE_PRESSURE SCALE_UP 1→2 (…)`) and last 8 task transitions with worker attribution

When the dispatcher rate-limits the client, a yellow `THROTTLED` banner appears and polling pauses for the `Retry-After` window. On connection failures a red `RECONNECTING` banner appears while the client backs off (2s doubling to a 30s cap); two minutes of continuous failure aborts the run.

## Report

After the run settles (or times out), the CLI prints:

- a verdict line (`PASS — all tasks completed`, `FAIL — run timeout with N unsettled task(s)`, …)
- per-executor counts: submitted, completed, failed, dead-lettered, unsettled
- latency percentiles p50/p95/max (submit → settled, COMPLETED tasks)
- the scaling timeline
- sample stdout (first 10 lines) from one completed task per executor

## Workload catalog

| Workload | Executor | What it does |
|----------|----------|--------------|
| `log-crunch` | SHELL | generates 2k–10k synthetic nginx access-log lines, computes a status-code histogram and top-5 paths |
| `backup-rotate` | SHELL | builds a fake config tree, tars and gzips it, reports the compression ratio, prunes old archives |
| `csv-report` | SHELL | generates a sales CSV, aggregates revenue per region in awk |
| `montecarlo-pi` | PYTHON | Monte Carlo π over 2–8M samples (CPU-bound) |
| `etl-json` | PYTHON | generates JSON orders, dedupes, computes per-customer aggregates and summary stats |
| `text-mining` | PYTHON | builds a zipf-ish corpus, counts word frequencies and top bigrams |
| `checksum-farm` | DOCKER | `alpine:3.20`; sha256 loop over generated 64KB blocks |
| `compress-bench` | DOCKER | `alpine:3.20`; compares gzip levels 1/6/9 on generated data |
| `sim-short` | SIMULATED | 2–5s simulated task |
| `sim-long` | SIMULATED | 10–20s simulated task |
| `sim-flaky` | SIMULATED | 40% failure per attempt; chaos only |

Every workload supports S/M/L sizes. The chaos scenario adds three private failure templates: `fail-python` (exits 1), `fail-shell` (exits 3), and `sim-timeout` (a fixed 20s task under a 10s timeout policy, which forces TIMED_OUT and, after one retry, DEAD_LETTERED).

The worker validates SHELL commands with a raw substring blocklist — the bare substring `dd` is blocked anywhere, even inside ordinary words. Workloads validate at build time, and a unit test renders every SHELL workload at every size to prove blocklist cleanliness.

PYTHON scripts use only the standard library: workers carry bare `python3`, and the executor creates a venv per task.

## Development

```bash
uv sync
uv run pytest            # 135 unit tests; no network
```

The unit suite covers the HTTP client (mock transport), workload spec generation and blocklist safety, scenario plan structure and seed reproducibility, monitor snapshot diffing and error handling, and report exit codes.

Integration tests run only against a live deployment:

```bash
CB_STRESS_URL=http://localhost uv run pytest tests/test_integration.py -v
```

They log in, query agents and scaling status, and submit one short SIMULATED task — safe against the dev compose or the two-machine deployment.

## Deployment notes

- **Two-machine setup:** run the CLI against the master's nginx origin (`--url http://<master-ip>`). The Task-10 master+slave simulation works with the default URL.
- **Rate limits:** the dispatcher allows the admin user 200 requests/minute. At defaults the CLI budgets ~100 req/min for polling plus ≤30 req/min for submissions, and fetches logs only at report time. On a 429 it honors `Retry-After` and shows `THROTTLED` — never a run failure.
- **Clock skew:** the task list filter compares server-assigned `submittedAt` timestamps; the CLI derives its polling floor from the first submission response minus a 5s margin, never from the local clock.
- **Proxies:** the client ignores `HTTP(S)_PROXY`/`ALL_PROXY` and talks to the origin directly.
