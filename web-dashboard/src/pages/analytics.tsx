import { useMemo, useState } from "react"
import { useStrategy } from "@/api/admin"
import { useClusterMetrics, useWorkerList, useWorkerHistory } from "@/api/workers"
import { Badge } from "@/components/ui/badge"
import { ErrorCard } from "@/components/error-card"
import { EmptyState } from "@/components/empty-state"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { BarChart3, DollarSign } from "lucide-react"
import { cn } from "@/lib/utils"

function cpuColor(pct: number): string {
  if (pct >= 80) return "bg-red-500"
  if (pct >= 60) return "bg-amber-500"
  if (pct >= 40) return "bg-blue-500"
  if (pct >= 20) return "bg-sky-400"
  return "bg-slate-700"
}

function HeatmapRow({ workerId }: { workerId: string }) {
  const now = useMemo(() => new Date().toISOString(), [])
  const oneHourAgo = useMemo(() => new Date(Date.now() - 3600_000).toISOString(), [])
  const { data } = useWorkerHistory(workerId, oneHourAgo, now, "5m")

  if (!data) return null

  return (
    <div className="flex items-center gap-1">
      <span className="w-28 font-mono text-[10px] text-slate-400 truncate shrink-0">{workerId}</span>
      <div className="flex gap-px flex-1">
        {data.map((bucket, i) => (
          <div
            key={i}
            className={cn("h-6 flex-1 rounded-sm min-w-[4px]", cpuColor(bucket.avgCpuPercent))}
            title={`${new Date(bucket.bucketStart).toLocaleTimeString()} — ${Math.round(bucket.avgCpuPercent)}% CPU`}
          />
        ))}
        {data.length === 0 && (
          <div className="h-6 flex-1 bg-slate-800 rounded-sm" />
        )}
      </div>
    </div>
  )
}

function LoadHeatmap() {
  const { data: workers, isLoading } = useWorkerList()

  const liveWorkers = useMemo(
    () => (workers ?? []).filter((w) => w.healthState !== "DEAD"),
    [workers],
  )

  if (isLoading) return <Skeleton className="h-48 w-full" />
  if (liveWorkers.length === 0) {
    return <p className="text-sm text-slate-500">No workers to display.</p>
  }

  return (
    <div className="space-y-1">
      {liveWorkers.map((w) => (
        <HeatmapRow key={w.id} workerId={w.id} />
      ))}
      {/* Legend */}
      <div className="flex items-center gap-2 pt-2">
        <span className="text-[10px] text-slate-500">0%</span>
        <div className="flex gap-px">
          {[
            "bg-slate-700", "bg-sky-400", "bg-blue-500", "bg-amber-500", "bg-red-500",
          ].map((c) => (
            <div key={c} className={cn("h-3 w-6 rounded-sm", c)} />
          ))}
        </div>
        <span className="text-[10px] text-slate-500">100%</span>
      </div>
    </div>
  )
}

function CostSimulator() {
  const { data: cluster } = useClusterMetrics()
  const { data: workers } = useWorkerList()
  const [costPerHour, setCostPerHour] = useState(0.10)

  const workerCount = useMemo(
    () => (workers ?? []).filter((w) => w.healthState !== "DEAD").length,
    [workers],
  )
  const throughput = cluster?.throughputPerMinute ?? 0

  const hourlyWorkerHours = workerCount
  const dailyCost = hourlyWorkerHours * costPerHour * 24
  const tasksPerDay = throughput * 60 * 24
  const costPerTask = tasksPerDay > 0 ? dailyCost / tasksPerDay : 0

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Label className="text-xs text-slate-400 whitespace-nowrap">Cost per worker-hour ($)</Label>
        <Input
          type="number"
          step="0.01"
          min="0"
          value={costPerHour}
          onChange={(e) => setCostPerHour(parseFloat(e.target.value) || 0)}
          className="w-24 h-8 text-xs bg-slate-800 border-slate-700"
        />
      </div>
      <div className="grid grid-cols-3 gap-3">
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Hourly Cost</p>
            <p className="text-lg font-mono">${(hourlyWorkerHours * costPerHour).toFixed(2)}</p>
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Projected Daily</p>
            <p className="text-lg font-mono">${dailyCost.toFixed(2)}</p>
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Cost per Task</p>
            <p className="text-lg font-mono">${costPerTask.toFixed(4)}</p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default function Analytics() {
  const strategy = useStrategy()
  const cluster = useClusterMetrics()
  const workerList = useWorkerList()

  const liveWorkerCount = useMemo(
    () => (workerList.data ?? []).filter((w) => w.healthState !== "DEAD").length,
    [workerList.data],
  )

  // Zero out stale metrics when no live workers
  const effectiveCpu = liveWorkerCount > 0 ? cluster.data?.avgCpuPercent ?? 0 : 0

  if (cluster.isError) return <ErrorCard error={cluster.error} onRetry={() => cluster.refetch()} />
  if (!cluster.isLoading && !cluster.data) {
    return <EmptyState icon={BarChart3} message="No metrics data yet. Data appears once workers report." />
  }

  return (
    <div className="space-y-8">
      <h1 className="text-xl font-semibold">Analytics</h1>

      {/* Strategy section */}
      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3 flex items-center gap-2">
          <BarChart3 className="h-4 w-4" /> Current Strategy
        </h2>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-4">
            {strategy.isLoading ? (
              <Skeleton className="h-6 w-32" />
            ) : (
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="text-sm font-mono">
                  {strategy.data?.strategy ?? "Unknown"}
                </Badge>
                {strategy.data?.weights && (
                  <div className="flex gap-2">
                    {Object.entries(strategy.data.weights).map(([key, val]) => (
                      <span key={key} className="text-xs text-slate-400">
                        {key}: <span className="font-mono">{val}</span>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}
            {cluster.data && (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4">
                <div>
                  <p className="text-xs text-slate-500">Throughput</p>
                  <p className="text-sm font-mono">{cluster.data.throughputPerMinute.toFixed(1)}/min</p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Avg Latency</p>
                  <p className="text-sm font-mono">{Math.round(cluster.data.avgExecutionDurationMs)}ms</p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Avg Queue Wait</p>
                  <p className="text-sm font-mono">{Math.round(cluster.data.avgQueueWaitMs)}ms</p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Cluster CPU</p>
                  <p className="text-sm font-mono">{Math.round(effectiveCpu)}%</p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      {/* Heatmap */}
      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Worker Load Heatmap (1h, 5m buckets)</h2>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-4">
            <LoadHeatmap />
          </CardContent>
        </Card>
      </section>

      {/* Cost simulator */}
      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3 flex items-center gap-2">
          <DollarSign className="h-4 w-4" /> Cost Simulator
        </h2>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-4">
            <CostSimulator />
          </CardContent>
        </Card>
      </section>
    </div>
  )
}
