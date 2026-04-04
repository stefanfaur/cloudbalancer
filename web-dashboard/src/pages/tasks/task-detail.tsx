import { useParams, Link } from "react-router-dom"
import { useTask, useTaskLogs, useBulkCancel, useBulkRetry } from "@/api/tasks"
import { useWorkerHistory } from "@/api/workers"
import { useAuth } from "@/hooks/use-auth"
import { StatusBadge } from "@/components/status-badge"
import { LifecycleTimeline } from "@/components/lifecycle-timeline"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Copy, ArrowLeft } from "lucide-react"
import { useState, useEffect, useRef, useCallback, memo } from "react"
import { cn } from "@/lib/utils"
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts"
import type { TaskState } from "@/api/types"

const CANCELLABLE: TaskState[] = ["RUNNING", "ASSIGNED", "PROVISIONING", "QUEUED"]
const RETRIABLE: TaskState[] = ["FAILED", "TIMED_OUT"]

function LogViewer({ taskId, stdout, stderr, isRunning }: { taskId: string; stdout: string; stderr: string; isRunning: boolean }) {
  const containerRef = useRef<HTMLPreElement>(null)
  const [liveLines, setLiveLines] = useState<string[]>([])
  const wsRef = useRef<WebSocket | null>(null)
  const { accessToken } = useAuth()

  useEffect(() => {
    if (!isRunning || !accessToken) return
    const wsUrl = (import.meta.env.VITE_WS_URL ?? "ws://localhost")
    const ws = new WebSocket(`${wsUrl}/api/tasks/${taskId}/logs/stream?token=${accessToken}`)
    wsRef.current = ws

    ws.onmessage = (event) => {
      setLiveLines((prev) => [...prev, event.data])
      if (containerRef.current) {
        containerRef.current.scrollTop = containerRef.current.scrollHeight
      }
    }

    return () => {
      ws.close()
      wsRef.current = null
    }
  }, [taskId, isRunning, accessToken])

  return (
    <div className="space-y-3">
      {(stdout || liveLines.length > 0) && (
        <div>
          <p className="text-xs text-slate-500 mb-1">stdout</p>
          <pre
            ref={containerRef}
            className="bg-slate-900 rounded-md p-3 text-xs font-mono text-slate-300 max-h-80 overflow-auto whitespace-pre-wrap"
          >
            {stdout}
            {liveLines.length > 0 && "\n" + liveLines.join("\n")}
          </pre>
        </div>
      )}
      {stderr && (
        <div>
          <p className="text-xs text-slate-500 mb-1">stderr</p>
          <pre className="bg-slate-900 rounded-md p-3 text-xs font-mono text-red-300 max-h-80 overflow-auto whitespace-pre-wrap">
            {stderr}
          </pre>
        </div>
      )}
      {!stdout && !stderr && liveLines.length === 0 && (
        <p className="text-sm text-slate-500">No logs available.</p>
      )}
    </div>
  )
}

const ResourceCharts = memo(function ResourceCharts({ workerId, from, to }: { workerId: string; from?: string; to?: string }) {
  const { data } = useWorkerHistory(workerId, from, to, "1m")

  if (!data || data.length === 0) {
    return <p className="text-sm text-slate-500">No resource data for this execution window.</p>
  }

  const chartData = data.map((b) => ({
    time: new Date(b.bucketStart).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    cpu: b.avgCpuPercent,
    memory: b.avgHeapUsedMB,
  }))

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <Card className="bg-slate-900 border-slate-700">
        <CardHeader className="pb-2"><CardTitle className="text-xs text-slate-400">CPU %</CardTitle></CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={160}>
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
        <CardHeader className="pb-2"><CardTitle className="text-xs text-slate-400">Memory (MB)</CardTitle></CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={160}>
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

export default function TaskDetail() {
  const { id } = useParams<{ id: string }>()
  const { role } = useAuth()
  const { data: task, isLoading, isError, refetch } = useTask(id!)
  const logs = useTaskLogs(id!)
  const bulkCancel = useBulkCancel()
  const bulkRetry = useBulkRetry()
  const [confirmAction, setConfirmAction] = useState<"cancel" | "retry" | null>(null)
  const canOperate = role === "ADMIN" || role === "OPERATOR"

  const copyId = useCallback(() => {
    if (id) navigator.clipboard.writeText(id)
  }, [id])

  const executeAction = async () => {
    if (!id || !confirmAction) return
    if (confirmAction === "cancel") await bulkCancel.mutateAsync([id])
    else await bulkRetry.mutateAsync([id])
    setConfirmAction(null)
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (isError || !task) {
    return (
      <div className="text-center py-12">
        <p className="text-red-400">Failed to load task.</p>
        <Button variant="ghost" size="sm" onClick={() => refetch()} className="mt-2">Retry</Button>
      </div>
    )
  }

  const lastAttempt = task.executionHistory[task.executionHistory.length - 1]
  const isRunning = task.state === "RUNNING"

  return (
    <div className="space-y-6">
      {/* Back link */}
      <Link to="/tasks" className="inline-flex items-center gap-1 text-sm text-slate-400 hover:text-slate-200">
        <ArrowLeft className="h-4 w-4" /> Back to tasks
      </Link>

      {/* Header */}
      <div className="flex flex-wrap items-start gap-3">
        <div className="flex items-center gap-2">
          <h1 className="font-mono text-sm text-slate-300 select-all">{task.id}</h1>
          <button onClick={copyId} className="text-slate-500 hover:text-slate-300">
            <Copy className="h-3.5 w-3.5" />
          </button>
        </div>
        <StatusBadge state={task.state} />
        <Badge variant="outline" className={cn("text-xs", {
          "text-red-400": task.descriptor.priority === "CRITICAL",
          "text-orange-400": task.descriptor.priority === "HIGH",
          "text-slate-300": !task.descriptor.priority || task.descriptor.priority === "NORMAL",
          "text-slate-500": task.descriptor.priority === "LOW",
        })}>
          {task.descriptor.priority ?? "NORMAL"}
        </Badge>
        <Badge variant="outline" className="text-xs text-slate-400">{task.descriptor.executorType}</Badge>
        {lastAttempt?.workerId && (
          <Link to={`/workers/${lastAttempt.workerId}`} className="text-xs font-mono text-blue-400 hover:underline">
            {lastAttempt.workerId}
          </Link>
        )}
      </div>

      {/* Action buttons */}
      {canOperate && (
        <div className="flex gap-2">
          {CANCELLABLE.includes(task.state) && (
            <Button size="sm" variant="destructive" onClick={() => setConfirmAction("cancel")}>Cancel</Button>
          )}
          {RETRIABLE.includes(task.state) && (
            <Button size="sm" variant="secondary" onClick={() => setConfirmAction("retry")}>Retry</Button>
          )}
        </div>
      )}

      {/* Lifecycle timeline */}
      <Card className="bg-slate-900 border-slate-700 overflow-x-auto">
        <CardContent className="p-4">
          <LifecycleTimeline executionHistory={task.executionHistory} currentState={task.state} />
        </CardContent>
      </Card>

      {/* Tabs */}
      <Tabs defaultValue="logs">
        <TabsList>
          <TabsTrigger value="logs">Logs</TabsTrigger>
          <TabsTrigger value="history">Execution History</TabsTrigger>
          <TabsTrigger value="resources">Resources</TabsTrigger>
          <TabsTrigger value="artifacts">Artifacts</TabsTrigger>
          <TabsTrigger value="json">Raw JSON</TabsTrigger>
        </TabsList>

        <TabsContent value="logs" className="mt-4">
          <LogViewer
            taskId={id!}
            stdout={logs.data?.stdout ?? ""}
            stderr={logs.data?.stderr ?? ""}
            isRunning={isRunning}
          />
        </TabsContent>

        <TabsContent value="history" className="mt-4">
          {task.executionHistory.length === 0 ? (
            <p className="text-sm text-slate-500">No execution attempts yet.</p>
          ) : (
            <div className="rounded-md border border-slate-700 overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-700 hover:bg-transparent">
                    <TableHead className="text-xs">#</TableHead>
                    <TableHead className="text-xs">Worker</TableHead>
                    <TableHead className="text-xs">Started</TableHead>
                    <TableHead className="text-xs">Completed</TableHead>
                    <TableHead className="text-xs">Exit Code</TableHead>
                    <TableHead className="text-xs">Failure Reason</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {task.executionHistory.map((attempt) => (
                    <TableRow key={attempt.attemptNumber} className="border-slate-700/50">
                      <TableCell className="text-xs">{attempt.attemptNumber}</TableCell>
                      <TableCell>
                        <Link to={`/workers/${attempt.workerId}`} className="text-xs font-mono text-blue-400 hover:underline">
                          {attempt.workerId.slice(0, 12)}
                        </Link>
                      </TableCell>
                      <TableCell className="text-xs text-slate-400">
                        {new Date(attempt.startedAt).toLocaleString()}
                      </TableCell>
                      <TableCell className="text-xs text-slate-400">
                        {attempt.completedAt ? new Date(attempt.completedAt).toLocaleString() : "—"}
                      </TableCell>
                      <TableCell className={cn("text-xs font-mono", attempt.exitCode === 0 ? "text-emerald-400" : "text-red-400")}>
                        {attempt.exitCode}
                      </TableCell>
                      <TableCell className="text-xs text-red-300 max-w-xs truncate">
                        {attempt.failureReason ?? "—"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </TabsContent>

        <TabsContent value="resources" className="mt-4">
          {lastAttempt ? (
            <ResourceCharts
              workerId={lastAttempt.workerId}
              from={lastAttempt.startedAt}
              to={lastAttempt.completedAt}
            />
          ) : (
            <p className="text-sm text-slate-500">No execution data available for resource charts.</p>
          )}
        </TabsContent>

        <TabsContent value="artifacts" className="mt-4">
          {task.descriptor.io?.outputArtifacts && task.descriptor.io.outputArtifacts.length > 0 ? (
            <ul className="space-y-2">
              {task.descriptor.io.outputArtifacts.map((a) => (
                <li key={a.name} className="flex items-center gap-3 bg-slate-900 rounded-md px-3 py-2">
                  <span className="text-sm font-mono text-slate-300">{a.name}</span>
                  <span className="text-xs text-slate-500">{a.path}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">No output artifacts defined.</p>
          )}
        </TabsContent>

        <TabsContent value="json" className="mt-4">
          <pre className="bg-slate-900 rounded-md p-4 text-xs font-mono text-slate-300 max-h-[500px] overflow-auto">
            {JSON.stringify(task, null, 2)}
          </pre>
        </TabsContent>
      </Tabs>

      {/* Confirmation dialog */}
      <Dialog open={!!confirmAction} onOpenChange={(open) => { if (!open) setConfirmAction(null) }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm {confirmAction}</DialogTitle>
            <DialogDescription>
              {confirmAction === "cancel" ? "Cancel this task? This cannot be undone." : "Retry this task?"}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
            <Button
              variant={confirmAction === "cancel" ? "destructive" : "default"}
              onClick={executeAction}
            >
              Confirm
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
