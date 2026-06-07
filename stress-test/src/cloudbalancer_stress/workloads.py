"""Workload catalog — realistic task templates for every executor type.

Each workload is pure data + a spec factory parameterized by size
(S/M/L) and a seeded random.Random. SHELL commands are validated against
the worker's blocklist at build time: the worker does a *raw substring*
match over the whole command string, so no rendered command may contain
`dd` anywhere — not even inside ordinary words.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from typing import Any, Callable

SIZES = ("S", "M", "L")

# Mirrors the worker ShellExecutor blocklist (substring match).
SHELL_BLOCKLIST = (
    "rm -rf /",
    "shutdown",
    "reboot",
    "mkfs",
    "dd",
    ":(){ :|:& };",
)


class BlocklistViolation(ValueError):
    """A rendered SHELL command contains a blocked substring."""


def validate_shell_command(command: str) -> None:
    for blocked in SHELL_BLOCKLIST:
        if blocked in command:
            raise BlocklistViolation(
                f"command contains blocked substring {blocked!r}"
            )


@dataclass(frozen=True)
class Workload:
    """A named task template bound to one executor type."""

    name: str
    executor_type: str
    description: str
    builder: Callable[[str, random.Random], dict[str, Any]]

    def build_spec(self, size: str, rng: random.Random) -> dict[str, Any]:
        if size not in SIZES:
            raise ValueError(f"unknown size {size!r}; expected one of {SIZES}")
        spec = self.builder(size, rng)
        if self.executor_type == "SHELL":
            validate_shell_command(spec["command"])
        return spec


# ---------------------------------------------------------------------------
# SHELL — ops-flavored pipelines
# ---------------------------------------------------------------------------

_LOG_LINES = {"S": 2000, "M": 5000, "L": 10000}


def _log_crunch(size: str, rng: random.Random) -> dict[str, Any]:
    lines = _LOG_LINES[size]
    seed = rng.randint(1, 99999)
    command = (
        f"awk 'BEGIN {{ srand({seed}); "
        'split("/index.html /api/users /api/orders /static/app.js /health /login /api/items /metrics", paths, " "); '
        'split("200 200 200 200 200 301 404 500 502", codes, " "); '
        f"for (i = 1; i <= {lines}; i++) "
        'printf "10.%d.%d.%d - - \\"GET %s HTTP/1.1\\" %s %d\\n", '
        "int(rand()*256), int(rand()*256), int(rand()*256), "
        "paths[int(rand()*8)+1], codes[int(rand()*9)+1], int(rand()*5000)+200 }' "
        "> /tmp/access.log; "
        'echo "== status code histogram =="; '
        "awk '{print $7}' /tmp/access.log | sort | uniq -c | sort -rn; "
        'echo "== top 5 paths =="; '
        "awk '{print $5}' /tmp/access.log | sort | uniq -c | sort -rn | head -5; "
        "rm -f /tmp/access.log"
    )
    return {"command": command}


_CONFIG_FILES = {"S": 15, "M": 40, "L": 80}


def _backup_rotate(size: str, rng: random.Random) -> dict[str, Any]:
    files = _CONFIG_FILES[size]
    seed = rng.randint(1, 99999)
    command = (
        'W=/tmp/backup_run_$$; mkdir -p "$W/etc" && cd "$W" || exit 1; '
        f"i=1; while [ $i -le {files} ]; do "
        f"awk -v n=$i 'BEGIN {{ srand({seed} + n); "
        'for (l = 1; l <= 200; l++) printf "key_%d_%d=value_%d\\n", n, l, int(rand()*100000) }\' '
        "> etc/app_$i.conf; i=$((i+1)); done; "
        "tar czf backup_1.tar.gz etc || exit 1; "
        "orig=$(du -sk etc | awk '{print $1}'); "
        "comp=$(wc -c < backup_1.tar.gz); "
        'echo "original ${orig}KB compressed ${comp}B"; '
        "n=1; while [ $n -le 6 ]; do cp backup_1.tar.gz archive_$n.tar.gz; n=$((n+1)); done; "
        'echo "pruning old archives, keeping newest 3"; '
        "ls -1 archive_*.tar.gz | head -3 | while read f; do rm -f \"$f\"; done; "
        'echo "archives remaining: $(ls -1 *.tar.gz | wc -l)"; '
        'cd /tmp && rm -r "$W"'
    )
    return {"command": command}


_CSV_ROWS = {"S": 3000, "M": 8000, "L": 20000}


def _csv_report(size: str, rng: random.Random) -> dict[str, Any]:
    rows = _CSV_ROWS[size]
    seed = rng.randint(1, 99999)
    command = (
        f"awk 'BEGIN {{ srand({seed}); "
        'split("north south east west", regions, " "); '
        'print "order_id,region,amount"; '
        f"for (i = 1; i <= {rows}; i++) "
        'printf "%d,%s,%d\\n", i, regions[int(rand()*4)+1], int(rand()*900)+100 }\' '
        "> /tmp/sales.csv; "
        'echo "== revenue by region =="; '
        "awk -F, 'NR > 1 { rev[$2] += $3; cnt[$2]++ } "
        'END { for (r in rev) printf "%-8s revenue=%d orders=%d avg=%.1f\\n", r, rev[r], cnt[r], rev[r]/cnt[r] }\' '
        "/tmp/sales.csv | sort; "
        "rm -f /tmp/sales.csv"
    )
    return {"command": command}


# ---------------------------------------------------------------------------
# PYTHON — data/science-flavored scripts (stdlib only)
# ---------------------------------------------------------------------------

_PI_SAMPLES = {"S": 2_000_000, "M": 4_000_000, "L": 8_000_000}


def _montecarlo_pi(size: str, rng: random.Random) -> dict[str, Any]:
    samples = _PI_SAMPLES[size]
    seed = rng.randint(1, 99999)
    script = f"""\
import random
import time

random.seed({seed})
samples = {samples}
start = time.monotonic()
inside = 0
for _ in range(samples):
    x = random.random()
    y = random.random()
    if x * x + y * y <= 1.0:
        inside += 1
pi_estimate = 4.0 * inside / samples
elapsed = time.monotonic() - start
print(f"samples={{samples}} pi={{pi_estimate:.6f}} elapsed={{elapsed:.2f}}s")
print(f"error={{abs(pi_estimate - 3.141592653589793):.6f}}")
"""
    return {"script": script, "networkAccessRequired": False}


_ORDER_COUNT = {"S": 5000, "M": 20000, "L": 50000}


def _etl_json(size: str, rng: random.Random) -> dict[str, Any]:
    orders = _ORDER_COUNT[size]
    seed = rng.randint(1, 99999)
    script = f"""\
import json
import random
import statistics
from collections import defaultdict

random.seed({seed})
n = {orders}
customers = [f"cust-{{i:04d}}" for i in range(200)]

# generate raw order records, ~10% duplicates
records = []
for order_id in range(n):
    records.append(json.dumps({{
        "orderId": order_id,
        "customer": random.choice(customers),
        "amount": round(random.uniform(5, 500), 2),
    }}))
dupes = random.sample(range(n), n // 10)
records.extend(records[i] for i in dupes)
random.shuffle(records)

# normalize + dedupe
seen = set()
clean = []
for raw in records:
    rec = json.loads(raw)
    if rec["orderId"] in seen:
        continue
    seen.add(rec["orderId"])
    clean.append(rec)

per_customer = defaultdict(list)
for rec in clean:
    per_customer[rec["customer"]].append(rec["amount"])

totals = sorted(
    ((cust, sum(amounts), len(amounts)) for cust, amounts in per_customer.items()),
    key=lambda t: t[1],
    reverse=True,
)
all_amounts = [rec["amount"] for rec in clean]
print(f"raw={{len(records)}} deduped={{len(clean)}} customers={{len(per_customer)}}")
print(f"revenue total={{sum(all_amounts):.2f}} mean={{statistics.mean(all_amounts):.2f}} "
      f"median={{statistics.median(all_amounts):.2f}}")
print("top 5 customers:")
for cust, total, count in totals[:5]:
    print(f"  {{cust}} total={{total:.2f}} orders={{count}}")
"""
    return {"script": script, "networkAccessRequired": False}


_CORPUS_WORDS = {"S": 50_000, "M": 150_000, "L": 400_000}


def _text_mining(size: str, rng: random.Random) -> dict[str, Any]:
    words = _CORPUS_WORDS[size]
    seed = rng.randint(1, 99999)
    script = f"""\
import random
from collections import Counter

random.seed({seed})
vocab = (
    "cloud balancer task worker queue scale node cluster metric log "
    "deploy retry fail run job batch stream event broker topic "
    "shell python docker container agent heartbeat poll state"
).split()
weights = [1.0 / (i + 1) for i in range(len(vocab))]  # zipf-ish

corpus = random.choices(vocab, weights=weights, k={words})
freq = Counter(corpus)
bigrams = Counter(zip(corpus, corpus[1:]))

print(f"corpus={{len(corpus)}} vocabulary={{len(freq)}}")
print("top 10 words:")
for word, count in freq.most_common(10):
    print(f"  {{word}}: {{count}}")
print("top 5 bigrams:")
for (first, second), count in bigrams.most_common(5):
    print(f"  {{first}} {{second}}: {{count}}")
"""
    return {"script": script, "networkAccessRequired": False}


# ---------------------------------------------------------------------------
# DOCKER — containerized batch jobs (alpine:3.20, no network)
# ---------------------------------------------------------------------------

_DOCKER_IMAGE = "alpine:3.20"
_DOCKER_MEMORY = 256 * 1024 * 1024  # 256 MiB

_CHECKSUM_BLOCKS = {"S": 10, "M": 25, "L": 60}


def _checksum_farm(size: str, rng: random.Random) -> dict[str, Any]:
    blocks = _CHECKSUM_BLOCKS[size]
    script = (
        f"i=1; while [ $i -le {blocks} ]; do "
        "head -c 65536 /dev/urandom > /tmp/block_$i.bin; "
        "sha256sum /tmp/block_$i.bin; "
        "i=$((i+1)); done; "
        f'echo "checksummed {blocks} blocks of 64KB"'
    )
    return {
        "image": _DOCKER_IMAGE,
        "command": ["sh", "-c", script],
        "memoryLimitBytes": _DOCKER_MEMORY,
        "cpuCount": 1,
        "networkDisabled": True,
    }


_COMPRESS_LINES = {"S": 200_000, "M": 500_000, "L": 1_000_000}


def _compress_bench(size: str, rng: random.Random) -> dict[str, Any]:
    lines = _COMPRESS_LINES[size]
    script = (
        f"seq 1 {lines} > /tmp/data.txt; "
        "orig=$(wc -c < /tmp/data.txt); "
        'echo "input ${orig}B"; '
        "for lvl in 1 6 9; do "
        "start=$(date +%s); "
        "gzip -c -$lvl /tmp/data.txt > /tmp/out.gz; "
        "end=$(date +%s); "
        "csize=$(wc -c < /tmp/out.gz); "
        'echo "level $lvl size ${csize}B seconds $((end - start))"; '
        "done"
    )
    return {
        "image": _DOCKER_IMAGE,
        "command": ["sh", "-c", script],
        "memoryLimitBytes": _DOCKER_MEMORY,
        "cpuCount": 1,
        "networkDisabled": True,
    }


# ---------------------------------------------------------------------------
# SIMULATED
# ---------------------------------------------------------------------------

_SIM_SHORT_RANGE = {"S": (2000, 3000), "M": (3000, 4000), "L": (4000, 5000)}
_SIM_LONG_RANGE = {"S": (10000, 13000), "M": (13000, 16000), "L": (16000, 20000)}


def _sim_short(size: str, rng: random.Random) -> dict[str, Any]:
    low, high = _SIM_SHORT_RANGE[size]
    return {"durationMs": rng.randint(low, high)}


def _sim_long(size: str, rng: random.Random) -> dict[str, Any]:
    low, high = _SIM_LONG_RANGE[size]
    return {"durationMs": rng.randint(low, high)}


def _sim_flaky(size: str, rng: random.Random) -> dict[str, Any]:
    low, high = _SIM_SHORT_RANGE[size]
    return {"durationMs": rng.randint(low, high), "failProbability": 0.4}


# ---------------------------------------------------------------------------
# Catalog
# ---------------------------------------------------------------------------

WORKLOADS: dict[str, Workload] = {
    w.name: w
    for w in (
        Workload("log-crunch", "SHELL", "nginx access-log histogram + top paths", _log_crunch),
        Workload("backup-rotate", "SHELL", "tar+gzip config tree, prune archives", _backup_rotate),
        Workload("csv-report", "SHELL", "sales CSV revenue aggregates", _csv_report),
        Workload("montecarlo-pi", "PYTHON", "Monte Carlo pi estimation (CPU-bound)", _montecarlo_pi),
        Workload("etl-json", "PYTHON", "JSON order ETL: dedupe + aggregates", _etl_json),
        Workload("text-mining", "PYTHON", "word frequencies + top bigrams", _text_mining),
        Workload("checksum-farm", "DOCKER", "sha256 loop over generated blocks", _checksum_farm),
        Workload("compress-bench", "DOCKER", "gzip level comparison benchmark", _compress_bench),
        Workload("sim-short", "SIMULATED", "2-5s simulated task", _sim_short),
        Workload("sim-long", "SIMULATED", "10-20s simulated task", _sim_long),
        Workload("sim-flaky", "SIMULATED", "flaky simulated task (40% failure)", _sim_flaky),
    )
}


def by_name(name: str) -> Workload:
    try:
        return WORKLOADS[name]
    except KeyError:
        raise KeyError(f"unknown workload {name!r}; known: {sorted(WORKLOADS)}") from None


def by_executor(executor_type: str) -> list[Workload]:
    return [w for w in WORKLOADS.values() if w.executor_type == executor_type]
