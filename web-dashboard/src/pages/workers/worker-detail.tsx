import { useParams, Link } from "react-router-dom"
import { useWorkerSnapshots, useWorkerHistory } from "@/api/workers"
import { useTasks } from "@/api/tasks"
import { useUpdateWorkerTags } from "@/api/admin"
import { useAuth } from "@/hooks/use-auth"
import { HealthBadge } from "@/components/health-badge"
import { StatusBadge } from "@/components/status-badge"
import { CpuGauge } from "@/components/cpu-gauge"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { ArrowLeft, Plus, X } from "lucide-react"
import { useState, memo } from "react"
import { formatDistanceToNow } from "date-fns"
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts"
import type { WorkerHealthState } from "@/api/types"

function deriveHealth(w: { cpuUsagePercent: number }): WorkerHealthState {
  if (w.cpuUsagePercent > 95) return "SUSPECT"
  return "HEALTHY"
}

const WorkerCharts = memo(function WorkerCharts({ workerId }: { workerId: string }) {
  const { data } = useWorkerHistory(workerId)

  if (!data || data.length === 0) {
    return <p className="text-sm text-slate-500">No history data yet. Data appears once the worker reports metrics.</p>
  }

  const chartData = data.map((b) => ({
    time: new Date(b.bucketStart).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    cpu: b.avgCpuPercent,
    memory: b.avgHeapUsedMB,
  }))

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <Card className="bg-slate-900 border-slate-700">
        <CardHeader className="pb-2"><CardTitle className="text-xs text-slate-400">CPU % (1h)</CardTitle></CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={192}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" tick={{ fontSize: 9, fill: "#94a3b8" }} />
              <YAxis domain={[0, 100]} tick={{ fontSize: 9, fill: "#94a3b8" }} />
              <Tooltip contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #334155", borderRadius: 8, fontSize: 11 }} />
              <Line type="monotone" dataKey="cpu" stroke="#3b82f6" dot={false} strokeWidth={1.5} />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
      <Card className="bg-slate-900 border-slate-700">
        <CardHeader className="pb-2"><CardTitle className="text-xs text-slate-400">Heap Used (MB) (1h)</CardTitle></CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={192}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" tick={{ fontSize: 9, fill: "#94a3b8" }} />
              <YAxis tick={{ fontSize: 9, fill: "#94a3b8" }} />
              <Tooltip contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #334155", borderRadius: 8, fontSize: 11 }} />
              <Line type="monotone" dataKey="memory" stroke="#8b5cf6" dot={false} strokeWidth={1.5} />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  )
})

function TagEditor({ workerId, initialTags }: { workerId: string; initialTags: string[] }) {
  const [tags, setTags] = useState(initialTags)
  const [newTag, setNewTag] = useState("")
  const updateTags = useUpdateWorkerTags()

  const addTag = async () => {
    if (!newTag.trim() || tags.includes(newTag.trim())) return
    const next = [...tags, newTag.trim()]
    setTags(next)
    setNewTag("")
    await updateTags.mutateAsync({ workerId, tags: next })
  }

  const removeTag = async (tag: string) => {
    const next = tags.filter((t) => t !== tag)
    setTags(next)
    await updateTags.mutateAsync({ workerId, tags: next })
  }

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {tags.map((tag) => (
        <Badge key={tag} variant="secondary" className="gap-1 text-xs">
          {tag}
          <button onClick={() => removeTag(tag)} className="hover:text-red-400">
            <X className="h-3 w-3" />
          </button>
        </Badge>
      ))}
      <div className="flex items-center gap-1">
        <input
          type="text"
          value={newTag}
          onChange={(e) => setNewTag(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter") addTag() }}
          placeholder="Add tag..."
          className="h-6 w-24 rounded border border-slate-700 bg-slate-800 px-1.5 text-xs text-slate-200 placeholder:text-slate-500"
        />
        <button onClick={addTag} className="text-slate-400 hover:text-slate-200">
          <Plus className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  )
}

export default function WorkerDetail() {
  const { id } = useParams<{ id: string }>()
  const { role } = useAuth()
  const { data: workers, isLoading } = useWorkerSnapshots()
  const tasks = useTasks({ workerId: id, limit: 10 })

  const worker = workers?.find((w) => w.workerId === id)
  const isAdmin = role === "ADMIN"

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (!worker) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-500">Worker not found.</p>
        <Link to="/workers" className="text-sm text-blue-400 hover:underline mt-2 inline-block">Back to workers</Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Link to="/workers" className="inline-flex items-center gap-1 text-sm text-slate-400 hover:text-slate-200">
        <ArrowLeft className="h-4 w-4" /> Back to workers
      </Link>

      {/* Header */}
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="font-mono text-sm text-slate-300">{worker.workerId}</h1>
        <HealthBadge state={deriveHealth(worker)} />
        <CpuGauge percent={worker.cpuUsagePercent} size={36} />
      </div>

      {/* Metrics summary */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">CPU</p>
            <p className="text-lg font-mono">{Math.round(worker.cpuUsagePercent)}%</p>
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Memory</p>
            <p className="text-lg font-mono">{Math.round(worker.heapUsedMB)}/{worker.heapMaxMB} MB</p>
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Active Tasks</p>
            <p className="text-lg font-mono">{worker.activeTaskCount}</p>
          </CardContent>
        </Card>
        <Card className="bg-slate-900 border-slate-700">
          <CardContent className="p-3">
            <p className="text-xs text-slate-500">Completed</p>
            <p className="text-lg font-mono">{worker.completedTaskCount}</p>
          </CardContent>
        </Card>
      </div>

      {/* Tags (ADMIN only) */}
      {isAdmin && (
        <div>
          <h2 className="text-sm font-medium text-slate-400 mb-2">Tags</h2>
          <TagEditor workerId={worker.workerId} initialTags={[]} />
        </div>
      )}

      {/* Charts */}
      <div>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Metrics History</h2>
        <WorkerCharts workerId={worker.workerId} />
      </div>

      {/* Recent tasks */}
      <div>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Recent Tasks</h2>
        {tasks.isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : tasks.data?.tasks && tasks.data.tasks.length > 0 ? (
          <div className="rounded-md border border-slate-700 overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="border-slate-700 hover:bg-transparent">
                  <TableHead className="text-xs">ID</TableHead>
                  <TableHead className="text-xs">Status</TableHead>
                  <TableHead className="text-xs">Executor</TableHead>
                  <TableHead className="text-xs">Submitted</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tasks.data.tasks.map((task) => (
                  <TableRow key={task.id} className="border-slate-700/50">
                    <TableCell>
                      <Link to={`/tasks/${task.id}`} className="font-mono text-xs text-blue-400 hover:underline">
                        {task.id.slice(0, 8)}
                      </Link>
                    </TableCell>
                    <TableCell><StatusBadge state={task.state} /></TableCell>
                    <TableCell className="text-xs text-slate-400">{task.descriptor.executorType}</TableCell>
                    <TableCell className="text-xs text-slate-400">
                      {formatDistanceToNow(new Date(task.submittedAt), { addSuffix: true })}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : (
          <p className="text-sm text-slate-500">No tasks assigned to this worker.</p>
        )}
      </div>
    </div>
  )
}
