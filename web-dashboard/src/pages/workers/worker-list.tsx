import { useNavigate } from "react-router-dom"
import { useWorkerList, useWorkerSnapshots, type WorkerSummary } from "@/api/workers"
import { HealthBadge } from "@/components/health-badge"
import { CpuGauge } from "@/components/cpu-gauge"
import { ErrorCard } from "@/components/error-card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Server } from "lucide-react"
import { useMemo, useState } from "react"
import type { WorkerHealthState, WorkerMetricsSnapshot } from "@/api/types"

type SortField = "workerId" | "health" | "cpu" | "memory" | "activeTasks"
type SortDir = "asc" | "desc"

interface MergedWorker {
  id: string
  healthState: WorkerHealthState
  agentId: string | null
  activeTaskCount: number
  registeredAt: string | null
  cpuUsagePercent: number | null
  heapUsedMB: number | null
  heapMaxMB: number | null
  threadCount: number | null
}

function mergeWorkers(
  workers: WorkerSummary[],
  snapshots: WorkerMetricsSnapshot[] | undefined,
): MergedWorker[] {
  const metricsMap = new Map<string, WorkerMetricsSnapshot>()
  if (snapshots) {
    for (const s of snapshots) metricsMap.set(s.workerId, s)
  }
  return workers.map((w) => {
    const m = metricsMap.get(w.id)
    return {
      id: w.id,
      healthState: w.healthState as WorkerHealthState,
      agentId: w.agentId,
      activeTaskCount: w.activeTaskCount,
      registeredAt: w.registeredAt,
      cpuUsagePercent: m?.cpuUsagePercent ?? null,
      heapUsedMB: m?.heapUsedMB ?? null,
      heapMaxMB: m?.heapMaxMB ?? null,
      threadCount: m?.threadCount ?? null,
    }
  })
}

export default function WorkerList() {
  const { data: workers, isLoading, isError, error, refetch } = useWorkerList()
  const { data: snapshots } = useWorkerSnapshots()
  const navigate = useNavigate()
  const [sortField, setSortField] = useState<SortField>("workerId")
  const [sortDir, setSortDir] = useState<SortDir>("asc")
  const [showDead, setShowDead] = useState(false)

  const merged = useMemo(
    () => mergeWorkers(workers ?? [], snapshots),
    [workers, snapshots],
  )

  const filtered = useMemo(
    () => (showDead ? merged : merged.filter((w) => w.healthState !== "DEAD")),
    [merged, showDead],
  )

  const deadCount = merged.length - merged.filter((w) => w.healthState !== "DEAD").length

  const toggleSort = (field: SortField) => {
    if (sortField === field) setSortDir((d) => (d === "asc" ? "desc" : "asc"))
    else { setSortField(field); setSortDir("asc") }
  }

  const sorted = [...filtered].sort((a, b) => {
    let cmp = 0
    if (sortField === "workerId") cmp = a.id.localeCompare(b.id)
    else if (sortField === "health") cmp = a.healthState.localeCompare(b.healthState)
    else if (sortField === "cpu") cmp = (a.cpuUsagePercent ?? 0) - (b.cpuUsagePercent ?? 0)
    else if (sortField === "memory") cmp = (a.heapUsedMB ?? 0) - (b.heapUsedMB ?? 0)
    else if (sortField === "activeTasks") cmp = a.activeTaskCount - b.activeTaskCount
    return sortDir === "asc" ? cmp : -cmp
  })

  const sortIndicator = (f: SortField) => sortField === f ? (sortDir === "asc" ? " ↑" : " ↓") : ""

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Workers</h1>
        {deadCount > 0 && (
          <label className="flex items-center gap-2 text-xs text-slate-400 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={showDead}
              onChange={(e) => setShowDead(e.target.checked)}
              className="rounded border-slate-600 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900"
            />
            Show dead ({deadCount})
          </label>
        )}
      </div>

      {isError ? (
        <ErrorCard error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
        </div>
      ) : sorted.length === 0 && !showDead ? (
        <div className="text-center py-12 text-sm text-slate-500">
          <Server className="h-8 w-8 mx-auto mb-2 text-slate-600" />
          {deadCount > 0
            ? `No active workers. ${deadCount} dead worker${deadCount > 1 ? "s" : ""} hidden.`
            : "No workers registered. Start a worker to begin."}
        </div>
      ) : (
        <div className="rounded-md border border-slate-700 overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-slate-700 hover:bg-transparent">
                <TableHead>
                  <button onClick={() => toggleSort("workerId")} className="text-xs hover:text-slate-200">
                    Worker ID{sortIndicator("workerId")}
                  </button>
                </TableHead>
                <TableHead>
                  <button onClick={() => toggleSort("health")} className="text-xs hover:text-slate-200">
                    Health{sortIndicator("health")}
                  </button>
                </TableHead>
                <TableHead className="text-xs">Agent</TableHead>
                <TableHead>
                  <button onClick={() => toggleSort("cpu")} className="text-xs hover:text-slate-200">
                    CPU %{sortIndicator("cpu")}
                  </button>
                </TableHead>
                <TableHead>
                  <button onClick={() => toggleSort("memory")} className="text-xs hover:text-slate-200">
                    Memory{sortIndicator("memory")}
                  </button>
                </TableHead>
                <TableHead>
                  <button onClick={() => toggleSort("activeTasks")} className="text-xs hover:text-slate-200">
                    Active Tasks{sortIndicator("activeTasks")}
                  </button>
                </TableHead>
                <TableHead className="text-xs">Threads</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((w) => (
                <TableRow
                  key={w.id}
                  className={`border-slate-700/50 cursor-pointer hover:bg-slate-800/50 ${w.healthState === "DEAD" ? "opacity-50" : ""}`}
                  onClick={() => navigate(`/workers/${w.id}`)}
                >
                  <TableCell className="font-mono text-xs text-slate-300">{w.id}</TableCell>
                  <TableCell><HealthBadge state={w.healthState} /></TableCell>
                  <TableCell className="text-xs font-mono text-slate-400">{w.agentId ?? "—"}</TableCell>
                  <TableCell>
                    {w.cpuUsagePercent != null ? (
                      <div className="flex items-center gap-2">
                        <CpuGauge percent={w.cpuUsagePercent} size={32} />
                        <span className="text-xs font-mono">{Math.round(w.cpuUsagePercent)}%</span>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-500">—</span>
                    )}
                  </TableCell>
                  <TableCell>
                    {w.heapUsedMB != null && w.heapMaxMB != null ? (
                      <div className="space-y-1">
                        <span className="text-xs font-mono">{Math.round(w.heapUsedMB)}/{w.heapMaxMB} MB</span>
                        <div className="h-1 w-20 bg-slate-700 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-blue-500 rounded-full"
                            style={{ width: `${Math.min((w.heapUsedMB / w.heapMaxMB) * 100, 100)}%` }}
                          />
                        </div>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-500">—</span>
                    )}
                  </TableCell>
                  <TableCell className="text-xs font-mono">{w.activeTaskCount}</TableCell>
                  <TableCell className="text-xs font-mono text-slate-400">{w.threadCount ?? "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
