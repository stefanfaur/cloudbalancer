import { useState, useMemo } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useTasks, useBulkCancel, useBulkRetry, useBulkReprioritize } from "@/api/tasks"
import { useAuth } from "@/hooks/use-auth"
import { StatusBadge } from "@/components/status-badge"
import { ErrorCard } from "@/components/error-card"
import { EmptyState } from "@/components/empty-state"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Plus, ChevronLeft, ChevronRight, ListChecks } from "lucide-react"
import { formatDistanceToNow } from "date-fns"
import { cn } from "@/lib/utils"
import type { Priority, TaskState, ExecutorType } from "@/api/types"

const STATUSES: TaskState[] = ["SUBMITTED", "VALIDATED", "QUEUED", "ASSIGNED", "PROVISIONING", "RUNNING", "POST_PROCESSING", "COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED", "DEAD_LETTERED"]
const PRIORITIES: Priority[] = ["CRITICAL", "HIGH", "NORMAL", "LOW"]
const EXECUTOR_TYPES: ExecutorType[] = ["SHELL", "DOCKER", "PYTHON", "SIMULATED"]
const TIME_RANGES = [
  { label: "1h", value: 1 },
  { label: "6h", value: 6 },
  { label: "24h", value: 24 },
  { label: "7d", value: 168 },
]
const PAGE_SIZE = 20

type SortField = "submittedAt" | "state" | "priority"
type SortDir = "asc" | "desc"

const PRIORITY_ORDER: Record<Priority, number> = { CRITICAL: 0, HIGH: 1, NORMAL: 2, LOW: 3 }

export default function TaskList() {
  const { role } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [confirmAction, setConfirmAction] = useState<{ type: "cancel" | "retry" | "reprioritize"; priority?: Priority } | null>(null)
  const [sortField, setSortField] = useState<SortField>("submittedAt")
  const [sortDir, setSortDir] = useState<SortDir>("desc")

  const status = searchParams.get("status") || ""
  const priority = searchParams.get("priority") || ""
  const executorType = searchParams.get("executorType") || ""
  const timeRange = searchParams.get("timeRange") || ""
  const search = searchParams.get("q") || ""
  const offset = parseInt(searchParams.get("offset") || "0", 10)

  const since = useMemo(() => {
    if (!timeRange) return undefined
    const hours = parseInt(timeRange, 10)
    return new Date(Date.now() - hours * 3600_000).toISOString()
  }, [timeRange])

  const filters = useMemo(() => ({
    offset,
    limit: PAGE_SIZE,
    status: status || undefined,
    priority: priority || undefined,
    executorType: executorType || undefined,
    since,
  }), [offset, status, priority, executorType, since])

  const { data, isLoading, isError, error, refetch } = useTasks(filters)
  const bulkCancel = useBulkCancel()
  const bulkRetry = useBulkRetry()
  const bulkReprioritize = useBulkReprioritize()

  const canOperate = role === "ADMIN" || role === "OPERATOR"

  // Client-side sort and search filter
  const sortedTasks = useMemo(() => {
    if (!data?.tasks) return []
    let tasks = [...data.tasks]

    // Filter by search (ID prefix)
    if (search) {
      const q = search.toLowerCase()
      tasks = tasks.filter((t) => t.id.toLowerCase().includes(q))
    }

    tasks.sort((a, b) => {
      let cmp = 0
      if (sortField === "submittedAt") {
        cmp = new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime()
      } else if (sortField === "state") {
        cmp = a.state.localeCompare(b.state)
      } else if (sortField === "priority") {
        const pa = a.descriptor.priority ?? "NORMAL"
        const pb = b.descriptor.priority ?? "NORMAL"
        cmp = PRIORITY_ORDER[pa] - PRIORITY_ORDER[pb]
      }
      return sortDir === "asc" ? cmp : -cmp
    })
    return tasks
  }, [data?.tasks, search, sortField, sortDir])

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"))
    } else {
      setSortField(field)
      setSortDir("asc")
    }
  }

  const toggleSelect = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const toggleAll = () => {
    if (selected.size === sortedTasks.length) {
      setSelected(new Set())
    } else {
      setSelected(new Set(sortedTasks.map((t) => t.id)))
    }
  }

  const updateParam = (key: string, value: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (value) next.set(key, value)
      else next.delete(key)
      if (key !== "offset") next.delete("offset")
      return next
    })
    setSelected(new Set())
  }

  const executeAction = async () => {
    if (!confirmAction) return
    const ids = Array.from(selected)
    if (confirmAction.type === "cancel") await bulkCancel.mutateAsync(ids)
    else if (confirmAction.type === "retry") await bulkRetry.mutateAsync(ids)
    else if (confirmAction.type === "reprioritize" && confirmAction.priority) {
      await bulkReprioritize.mutateAsync({ taskIds: ids, priority: confirmAction.priority })
    }
    setSelected(new Set())
    setConfirmAction(null)
  }

  if (isError) return <ErrorCard error={error} onRetry={() => refetch()} />
  if (!isLoading && data?.tasks.length === 0 && !status && !priority && !executorType && !search) {
    return <EmptyState icon={ListChecks} message="No tasks yet. Submit your first task." actionLabel="Submit Task" actionHref="/tasks/submit" />
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Tasks</h1>
        {canOperate && (
          <Link to="/tasks/submit">
            <Button size="sm">
              <Plus className="h-4 w-4 mr-1" />
              Submit Task
            </Button>
          </Link>
        )}
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-2">
        <select
          value={status}
          onChange={(e) => updateParam("status", e.target.value)}
          className="h-8 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select
          value={priority}
          onChange={(e) => updateParam("priority", e.target.value)}
          className="h-8 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200"
        >
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
        <select
          value={executorType}
          onChange={(e) => updateParam("executorType", e.target.value)}
          className="h-8 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200"
        >
          <option value="">All executors</option>
          {EXECUTOR_TYPES.map((e) => <option key={e} value={e}>{e}</option>)}
        </select>
        <select
          value={timeRange}
          onChange={(e) => updateParam("timeRange", e.target.value)}
          className="h-8 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200"
        >
          <option value="">All time</option>
          {TIME_RANGES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <input
          type="text"
          value={search}
          onChange={(e) => updateParam("q", e.target.value)}
          placeholder="Search by ID..."
          className="h-8 w-48 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200 placeholder:text-slate-500"
        />
      </div>

      {/* Bulk action bar */}
      {selected.size > 0 && canOperate && (
        <div className="flex items-center gap-2 bg-slate-800 rounded-md px-3 py-2 text-sm">
          <span className="text-slate-300">{selected.size} selected</span>
          <Button size="sm" variant="destructive" onClick={() => setConfirmAction({ type: "cancel" })}>
            Cancel Selected
          </Button>
          <Button size="sm" variant="secondary" onClick={() => setConfirmAction({ type: "retry" })}>
            Retry Selected
          </Button>
          <select
            onChange={(e) => {
              if (e.target.value) setConfirmAction({ type: "reprioritize", priority: e.target.value as Priority })
            }}
            className="h-8 rounded-md border border-slate-700 bg-slate-900 px-2 text-xs text-slate-200"
            defaultValue=""
          >
            <option value="" disabled>Re-prioritize...</option>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
        </div>
      )}

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      ) : (
        <>
          <div className="rounded-md border border-slate-700 overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="border-slate-700 hover:bg-transparent">
                  {canOperate && (
                    <TableHead className="w-10">
                      <Checkbox
                        checked={sortedTasks.length > 0 && selected.size === sortedTasks.length}
                        onCheckedChange={toggleAll}
                      />
                    </TableHead>
                  )}
                  <TableHead className="font-mono text-xs">ID</TableHead>
                  <TableHead>
                    <button onClick={() => toggleSort("state")} className="flex items-center gap-1 text-xs hover:text-slate-200">
                      Status {sortField === "state" && (sortDir === "asc" ? "↑" : "↓")}
                    </button>
                  </TableHead>
                  <TableHead>
                    <button onClick={() => toggleSort("priority")} className="flex items-center gap-1 text-xs hover:text-slate-200">
                      Priority {sortField === "priority" && (sortDir === "asc" ? "↑" : "↓")}
                    </button>
                  </TableHead>
                  <TableHead className="text-xs">Executor</TableHead>
                  <TableHead className="text-xs">Worker</TableHead>
                  <TableHead>
                    <button onClick={() => toggleSort("submittedAt")} className="flex items-center gap-1 text-xs hover:text-slate-200">
                      Submitted {sortField === "submittedAt" && (sortDir === "asc" ? "↑" : "↓")}
                    </button>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedTasks.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={canOperate ? 7 : 6} className="text-center py-12 text-slate-500">
                      No tasks found matching filters.
                    </TableCell>
                  </TableRow>
                ) : (
                  sortedTasks.map((task) => {
                    const lastAttempt = task.executionHistory[task.executionHistory.length - 1]
                    return (
                      <TableRow key={task.id} className="border-slate-700/50">
                        {canOperate && (
                          <TableCell>
                            <Checkbox
                              checked={selected.has(task.id)}
                              onCheckedChange={() => toggleSelect(task.id)}
                            />
                          </TableCell>
                        )}
                        <TableCell>
                          <Link
                            to={`/tasks/${task.id}`}
                            className="font-mono text-xs text-blue-400 hover:underline"
                          >
                            {task.id.slice(0, 8)}
                          </Link>
                        </TableCell>
                        <TableCell><StatusBadge state={task.state} /></TableCell>
                        <TableCell>
                          <Badge variant="outline" className={cn("text-xs", {
                            "text-red-400": task.descriptor.priority === "CRITICAL",
                            "text-orange-400": task.descriptor.priority === "HIGH",
                            "text-slate-300": task.descriptor.priority === "NORMAL" || !task.descriptor.priority,
                            "text-slate-500": task.descriptor.priority === "LOW",
                          })}>
                            {task.descriptor.priority ?? "NORMAL"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs text-slate-400">{task.descriptor.executorType}</TableCell>
                        <TableCell>
                          {lastAttempt?.workerId ? (
                            <Link to={`/workers/${lastAttempt.workerId}`} className="text-xs font-mono text-blue-400 hover:underline">
                              {lastAttempt.workerId.slice(0, 12)}
                            </Link>
                          ) : (
                            <span className="text-xs text-slate-600">—</span>
                          )}
                        </TableCell>
                        <TableCell className="text-xs text-slate-400">
                          {formatDistanceToNow(new Date(task.submittedAt), { addSuffix: true })}
                        </TableCell>
                      </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          {data && (
            <div className="flex items-center justify-between text-sm text-slate-400">
              <span>
                Showing {offset + 1}–{Math.min(offset + PAGE_SIZE, data.total)} of {data.total}
              </span>
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={offset === 0}
                  onClick={() => updateParam("offset", String(Math.max(0, offset - PAGE_SIZE)))}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={offset + PAGE_SIZE >= data.total}
                  onClick={() => updateParam("offset", String(offset + PAGE_SIZE))}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Confirmation dialog */}
      <Dialog open={!!confirmAction} onOpenChange={(open) => { if (!open) setConfirmAction(null) }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm {confirmAction?.type}</DialogTitle>
            <DialogDescription>
              {confirmAction?.type === "cancel" && `Cancel ${selected.size} task(s)? This cannot be undone.`}
              {confirmAction?.type === "retry" && `Retry ${selected.size} task(s)?`}
              {confirmAction?.type === "reprioritize" && `Re-prioritize ${selected.size} task(s) to ${confirmAction.priority}?`}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
            <Button
              variant={confirmAction?.type === "cancel" ? "destructive" : "default"}
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
