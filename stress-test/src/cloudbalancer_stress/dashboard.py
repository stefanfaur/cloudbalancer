"""Dashboard — Rich rendering of the live run state.

Single-shot: render(view) returns a Panel; the CLI wraps it in
rich.live.Live. The dashboard only reads RunStateView copies — it never
touches monitor internals.

Semantics follow the operational color conventions: green = success,
red = failure, yellow = queued/waiting, cyan = active, dim = inactive.
"""

from __future__ import annotations

import time
from typing import Callable

from rich.console import Group, RenderableType
from rich.panel import Panel
from rich.progress_bar import ProgressBar
from rich.table import Table
from rich.text import Text

from .monitor import (
    CountChangeEvent,
    RunStateView,
    TaskTransitionEvent,
)

# Display order for the full TaskState enum + local states; anything the
# server adds later lands in the trailing "other" bucket verbatim.
_STATE_ORDER = (
    "SUBMITTED",
    "VALIDATED",
    "QUEUED",
    "ASSIGNED",
    "PROVISIONING",
    "RUNNING",
    "POST_PROCESSING",
    "COMPLETED",
    "FAILED",
    "TIMED_OUT",
    "CANCELLED",
    "DEAD_LETTERED",
    "SUBMIT_FAILED",
)

_STATE_STYLES = {
    "QUEUED": "yellow",
    "RUNNING": "cyan",
    "COMPLETED": "green",
    "FAILED": "red",
    "TIMED_OUT": "dark_orange",
    "CANCELLED": "magenta",
    "DEAD_LETTERED": "red bold",
    "SUBMIT_FAILED": "red",
}

_EXECUTOR_ORDER = ("SHELL", "PYTHON", "DOCKER", "SIMULATED")


def _style_state(state: str) -> str:
    return _STATE_STYLES.get(state, "white")


class Dashboard:
    def __init__(
        self,
        scenario_name: str,
        target_url: str,
        planned_by_executor: dict[str, int],
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self._scenario = scenario_name
        self._url = target_url
        self._planned = planned_by_executor
        self._clock = clock
        self._start = clock()

    # -- sections -----------------------------------------------------------

    def _header(self, view: RunStateView) -> Text:
        elapsed = self._clock() - self._start
        minutes, seconds = divmod(int(elapsed), 60)
        total_planned = sum(self._planned.values())
        header = Text()
        header.append(f" {self._scenario} ", style="bold white on dark_blue")
        header.append(f"  {self._url}", style="dim")
        header.append(f"  elapsed {minutes:02d}:{seconds:02d}", style="cyan")
        header.append(f"  tasks {len(view.tasks)}/{total_planned}")
        if view.scaling is not None:
            header.append(
                f"  workers {view.scaling.worker_count}"
                f" ({view.scaling.active_worker_count} active)",
                style="bright_blue",
            )
        metrics = view.cluster_metrics
        if metrics is not None and metrics.avg_cpu_percent is not None:
            header.append(f"  cluster cpu {metrics.avg_cpu_percent:.0f}%", style="cyan")
        return header

    def _banner(self, view: RunStateView) -> RenderableType | None:
        if view.status == "THROTTLED":
            return Panel(
                Text(f"THROTTLED — {view.status_detail}", style="bold black"),
                style="on yellow",
                height=3,
            )
        if view.status == "RECONNECTING":
            return Panel(
                Text(f"RECONNECTING — {view.status_detail}", style="bold white"),
                style="on red",
                height=3,
            )
        return None

    def _state_counts(self, view: RunStateView) -> Text:
        counts = view.state_counts
        line = Text()
        for state in _STATE_ORDER:
            count = counts.pop(state, 0)
            if count:
                line.append(f"{state} ", style=_style_state(state))
                line.append(f"{count}  ", style=f"bold {_style_state(state)}")
        for state, count in sorted(counts.items()):  # unknown states, verbatim
            line.append(f"{state} ", style="white")
            line.append(f"{count}  ", style="bold white")
        if not line.plain:
            line.append("waiting for first submission…", style="dim")
        return line

    def _executor_progress(self, view: RunStateView) -> Table:
        grid = Table.grid(padding=(0, 1))
        grid.add_column(justify="right", min_width=10)
        grid.add_column(min_width=30)
        grid.add_column(justify="left")
        by_executor: dict[str, list] = {}
        for task in view.tasks.values():
            by_executor.setdefault(task.executor_type, []).append(task)
        for executor in _EXECUTOR_ORDER:
            planned = self._planned.get(executor, 0)
            if not planned:
                continue
            tasks = by_executor.get(executor, [])
            settled = sum(1 for t in tasks if t.settled)
            bar = ProgressBar(total=planned, completed=settled, width=30)
            grid.add_row(
                Text(executor, style="bold"),
                bar,
                Text(f"{settled}/{planned} settled", style="dim"),
            )
        return grid

    def _agents_table(self, view: RunStateView) -> Table:
        table = Table(
            title="agents",
            title_style="bold dim",
            show_edge=False,
            pad_edge=False,
            header_style="dim",
        )
        table.add_column("agent")
        table.add_column("host")
        table.add_column("cpu (avail/total)", justify="right")
        table.add_column("mem MB (avail/total)", justify="right")
        table.add_column("workers", justify="right")
        table.add_column("executors")
        if not view.agents:
            table.add_row(Text("no agents registered", style="red"), "", "", "", "", "")
            return table
        for agent in view.agents:
            table.add_row(
                agent.agent_id[:16],
                agent.hostname,
                f"{agent.available_cpu_cores:.1f}/{agent.total_cpu_cores:.0f}",
                f"{agent.available_memory_mb:.0f}/{agent.total_memory_mb:.0f}",
                str(len(agent.active_worker_ids)),
                " ".join(e[:3] for e in sorted(agent.supported_executors)),
            )
        return table

    def _event_log(self, view: RunStateView) -> Group:
        lines: list[Text] = []

        scaling_events = view.scaling_events[-5:]
        if scaling_events:
            lines.append(Text("scaling", style="bold dim"))
            for event in scaling_events:
                style = "green" if event.action == "SCALE_UP" else "dark_orange"
                lines.append(Text(f"  ⤷ {event.render()}", style=style))

        recent = [
            e
            for e in view.events
            if isinstance(e, (TaskTransitionEvent, CountChangeEvent))
        ][-8:]
        if recent:
            lines.append(Text("recent", style="bold dim"))
            for event in recent:
                if isinstance(event, TaskTransitionEvent):
                    line = Text("  ")
                    line.append(f"{event.workload_name}", style="bold")
                    line.append(f" [{event.task_id[:8]}] ", style="dim")
                    line.append(event.from_state, style=_style_state(event.from_state))
                    line.append(" → ")
                    line.append(event.to_state, style=_style_state(event.to_state))
                    if event.worker_id:
                        line.append(f" on {event.worker_id[:12]}", style="dim")
                    lines.append(line)
                else:
                    lines.append(
                        Text(
                            f"  {event.kind}: {event.previous} → {event.current}",
                            style="bright_blue",
                        )
                    )
        if not lines:
            lines.append(Text("no events yet", style="dim"))
        return Group(*lines)

    # -- single-shot render ---------------------------------------------------

    def render(self, view: RunStateView) -> Panel:
        sections: list[RenderableType] = [self._header(view)]
        banner = self._banner(view)
        if banner is not None:
            sections.append(banner)
        sections.append(Text())
        sections.append(self._state_counts(view))
        sections.append(Text())
        sections.append(self._executor_progress(view))
        sections.append(Text())
        sections.append(self._agents_table(view))
        sections.append(Text())
        sections.append(self._event_log(view))
        return Panel(
            Group(*sections),
            title="[bold]CloudBalancer stress[/bold]",
            border_style="dark_blue",
        )
