import { useNavigate } from "react-router-dom"
import { useClusterMetrics, useWorkerSnapshots, useWorkerList } from "@/api/workers"
import { KpiCard } from "@/components/kpi-card"
import { HealthBadge } from "@/components/health-badge"
import { CpuGauge } from "@/components/cpu-gauge"
import { ErrorCard } from "@/components/error-card"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Server,
  ListChecks,
  Cpu,
  MemoryStick,
  Gauge,
} from "lucide-react"
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts"
import { useEffect, useMemo, useRef, useState, memo } from "react"
import type { WorkerHealthState, WorkerMetricsSnapshot } from "@/api/types"

// Accumulate cluster metrics for time-series charts
function useMetricsHistory(cpuPercent: number | undefined, throughput: number | undefined) {
  const [history, setHistory] = useState<Array<{ time: string; cpu: number; throughput: number }>>([])
  const prevCpu = useRef<number>(undefined)

  useEffect(() => {
    if (cpuPercent === undefined || throughput === undefined) return
    // Only add if value actually changed (new data point)
    if (prevCpu.current === cpuPercent && history.length > 0) return
    prevCpu.current = cpuPercent

    setHistory((prev) => {
      const next = [...prev, {
        time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
        cpu: cpuPercent,
        throughput,
      }]
      return next.slice(-60) // keep last 60 data points
    })
  }, [cpuPercent, throughput]) // eslint-disable-line react-hooks/exhaustive-deps

  return history
}

const ClusterCpuChart = memo(function ClusterCpuChart({ data }: { data: Array<{ time: string; cpu: number }> }) {
  if (data.length === 0) {
    return (
      <div className="h-48 flex items-center justify-center text-sm text-slate-500">
        Waiting for data...
      </div>
    )
  }
  return (
    <ResponsiveContainer width="100%" height={192}>
      <AreaChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="time" tick={{ fontSize: 10, fill: "#94a3b8" }} />
        <YAxis domain={[0, 100]} tick={{ fontSize: 10, fill: "#94a3b8" }} tickFormatter={(v) => `${v}%`} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #334155", borderRadius: 8, fontSize: 12 }}
          labelStyle={{ color: "#94a3b8" }}
        />
        <Area type="monotone" dataKey="cpu" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.15} name="CPU %" />
      </AreaChart>
    </ResponsiveContainer>
  )
})

const ThroughputChart = memo(function ThroughputChart({ data }: { data: Array<{ time: string; throughput: number }> }) {
  if (data.length === 0) {
    return (
      <div className="h-48 flex items-center justify-center text-sm text-slate-500">
        Waiting for data...
      </div>
    )
  }
  return (
    <ResponsiveContainer width="100%" height={192}>
      <AreaChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="time" tick={{ fontSize: 10, fill: "#94a3b8" }} />
        <YAxis tick={{ fontSize: 10, fill: "#94a3b8" }} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #334155", borderRadius: 8, fontSize: 12 }}
          labelStyle={{ color: "#94a3b8" }}
        />
        <Area type="monotone" dataKey="throughput" stroke="#10b981" fill="#10b981" fillOpacity={0.15} name="Tasks/min" />
      </AreaChart>
    </ResponsiveContainer>
  )
})

function KpiSkeleton() {
  return (
    <Card className="bg-slate-900 border-slate-700">
      <CardContent className="flex items-center gap-4 p-4">
        <Skeleton className="h-10 w-10 rounded-lg" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-6 w-16" />
          <Skeleton className="h-3 w-24" />
        </div>
      </CardContent>
    </Card>
  )
}

function WorkerCardSkeleton() {
  return (
    <Card className="bg-slate-900 border-slate-700">
      <CardContent className="p-4 space-y-3">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-5 w-20" />
        <div className="flex gap-4">
          <Skeleton className="h-12 w-12 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-2/3" />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

interface MergedWorker {
  id: string
  healthState: WorkerHealthState
  cpuUsagePercent: number | null
  heapUsedMB: number | null
  heapMaxMB: number | null
  activeTaskCount: number
}

export default function ClusterOverview() {
  const cluster = useClusterMetrics()
  const snapshots = useWorkerSnapshots()
  const workerList = useWorkerList()
  const navigate = useNavigate()

  // Merge dispatcher worker list (source of truth) with metrics snapshots
  const metricsMap = useMemo(() => {
    const m = new Map<string, WorkerMetricsSnapshot>()
    if (snapshots.data) {
      for (const s of snapshots.data) m.set(s.workerId, s)
    }
    return m
  }, [snapshots.data])

  const liveWorkers: MergedWorker[] = useMemo(() => {
    if (!workerList.data) return []
    return workerList.data
      .filter((w) => w.healthState !== "DEAD")
      .map((w) => {
        const m = metricsMap.get(w.id)
        return {
          id: w.id,
          healthState: w.healthState as WorkerHealthState,
          cpuUsagePercent: m?.cpuUsagePercent ?? null,
          heapUsedMB: m?.heapUsedMB ?? null,
          heapMaxMB: m?.heapMaxMB ?? null,
          activeTaskCount: w.activeTaskCount,
        }
      })
  }, [workerList.data, metricsMap])

  const totalWorkerCount = liveWorkers.length
  const healthyCount = liveWorkers.filter(
    (w) => w.healthState === "HEALTHY" || w.healthState === "RECOVERING",
  ).length

  // If no live workers, zero out stale metrics
  const effectiveCpu = totalWorkerCount > 0 ? cluster.data?.avgCpuPercent : 0
  const effectiveHeap = totalWorkerCount > 0 ? cluster.data?.totalHeapUsedMB : 0

  const metricsHistory = useMetricsHistory(effectiveCpu, cluster.data?.throughputPerMinute)

  if (cluster.isError) return <ErrorCard error={cluster.error} onRetry={() => cluster.refetch()} />

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Cluster Overview</h1>

      {/* KPI cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-3">
        {cluster.isLoading || workerList.isLoading ? (
          Array.from({ length: 6 }).map((_, i) => <KpiSkeleton key={i} />)
        ) : cluster.data ? (
          <>
            <KpiCard label="Total Workers" value={totalWorkerCount} icon={Server} />
            <KpiCard label="Active Tasks" value={cluster.data.totalActiveTaskCount} icon={ListChecks} />
            <KpiCard
              label="Healthy Workers"
              value={`${healthyCount}/${totalWorkerCount}`}
              icon={Server}
              trend={totalWorkerCount > 0 && healthyCount === totalWorkerCount ? "up" : totalWorkerCount > 0 ? "down" : undefined}
            />
            <KpiCard label="Cluster CPU" value={`${Math.round(effectiveCpu ?? 0)}%`} icon={Cpu} />
            <KpiCard label="Heap Used" value={`${Math.round(effectiveHeap ?? 0)} MB`} icon={MemoryStick} />
            <KpiCard label="Throughput" value={`${cluster.data.throughputPerMinute.toFixed(1)}/min`} icon={Gauge} />
          </>
        ) : null}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card className="bg-slate-900 border-slate-700">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-slate-400">Cluster CPU %</CardTitle>
          </CardHeader>
          <CardContent>
            <ClusterCpuChart data={metricsHistory} />
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-slate-400">Task Throughput</CardTitle>
          </CardHeader>
          <CardContent>
            <ThroughputChart data={metricsHistory} />
          </CardContent>
        </Card>
      </div>

      {/* Worker cards grid */}
      <div>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Workers</h2>
        {workerList.isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
            {Array.from({ length: 4 }).map((_, i) => <WorkerCardSkeleton key={i} />)}
          </div>
        ) : liveWorkers.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
            {liveWorkers.map((w) => (
              <Card
                key={w.id}
                className="bg-slate-900 border-slate-700 cursor-pointer hover:border-slate-500 transition-colors"
                onClick={() => navigate(`/workers/${w.id}`)}
              >
                <CardContent className="p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs text-slate-300 truncate max-w-[140px]">
                      {w.id}
                    </span>
                    <HealthBadge state={w.healthState} />
                  </div>
                  <div className="flex items-center gap-4">
                    <CpuGauge percent={w.cpuUsagePercent ?? 0} />
                    <div className="flex-1 space-y-1.5 text-xs">
                      <div className="flex justify-between">
                        <span className="text-slate-500">Memory</span>
                        <span className="font-mono">
                          {w.heapUsedMB != null && w.heapMaxMB != null
                            ? `${Math.round(w.heapUsedMB)}/${w.heapMaxMB} MB`
                            : "—"}
                        </span>
                      </div>
                      <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-blue-500 rounded-full transition-all"
                          style={{ width: `${w.heapUsedMB != null && w.heapMaxMB ? Math.min((w.heapUsedMB / w.heapMaxMB) * 100, 100) : 0}%` }}
                        />
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-500">Active tasks</span>
                        <span className="font-mono">{w.activeTaskCount}</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-sm text-slate-500">
            <Server className="h-8 w-8 mx-auto mb-2 text-slate-600" />
            No workers registered. Start a worker to begin.
          </div>
        )}
      </div>
    </div>
  )
}
