import { useNavigate } from "react-router-dom"
import { useWorkerSnapshots } from "@/api/workers"
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
import { useState } from "react"
import type { WorkerHealthState } from "@/api/types"

type SortField = "workerId" | "cpu" | "memory" | "activeTasks"
type SortDir = "asc" | "desc"

function deriveHealth(w: { cpuUsagePercent: number }): WorkerHealthState {
  if (w.cpuUsagePercent > 95) return "SUSPECT"
  return "HEALTHY"
}

export default function WorkerList() {
  const { data: workers, isLoading, isError, error, refetch } = useWorkerSnapshots()
  const navigate = useNavigate()
  const [sortField, setSortField] = useState<SortField>("workerId")
  const [sortDir, setSortDir] = useState<SortDir>("asc")

  const toggleSort = (field: SortField) => {
    if (sortField === field) setSortDir((d) => (d === "asc" ? "desc" : "asc"))
    else { setSortField(field); setSortDir("asc") }
  }

  const sorted = [...(workers ?? [])].sort((a, b) => {
    let cmp = 0
    if (sortField === "workerId") cmp = a.workerId.localeCompare(b.workerId)
    else if (sortField === "cpu") cmp = a.cpuUsagePercent - b.cpuUsagePercent
    else if (sortField === "memory") cmp = a.heapUsedMB - b.heapUsedMB
    else if (sortField === "activeTasks") cmp = a.activeTaskCount - b.activeTaskCount
    return sortDir === "asc" ? cmp : -cmp
  })

  const sortIndicator = (f: SortField) => sortField === f ? (sortDir === "asc" ? " ↑" : " ↓") : ""

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Workers</h1>

      {isError ? (
        <ErrorCard error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
        </div>
      ) : sorted.length === 0 ? (
        <div className="text-center py-12 text-sm text-slate-500">
          <Server className="h-8 w-8 mx-auto mb-2 text-slate-600" />
          No workers registered. Start a worker to begin.
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
                <TableHead className="text-xs">Health</TableHead>
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
                  key={w.workerId}
                  className="border-slate-700/50 cursor-pointer hover:bg-slate-800/50"
                  onClick={() => navigate(`/workers/${w.workerId}`)}
                >
                  <TableCell className="font-mono text-xs text-slate-300">{w.workerId}</TableCell>
                  <TableCell><HealthBadge state={deriveHealth(w)} /></TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <CpuGauge percent={w.cpuUsagePercent} size={32} />
                      <span className="text-xs font-mono">{Math.round(w.cpuUsagePercent)}%</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <span className="text-xs font-mono">{Math.round(w.heapUsedMB)}/{w.heapMaxMB} MB</span>
                      <div className="h-1 w-20 bg-slate-700 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-blue-500 rounded-full"
                          style={{ width: `${Math.min((w.heapUsedMB / w.heapMaxMB) * 100, 100)}%` }}
                        />
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-xs font-mono">{w.activeTaskCount}</TableCell>
                  <TableCell className="text-xs font-mono text-slate-400">{w.threadCount}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
