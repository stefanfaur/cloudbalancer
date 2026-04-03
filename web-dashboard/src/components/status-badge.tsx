import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { TaskState } from "@/api/types"

const STATE_CONFIG: Record<TaskState, { color: string; dot: string }> = {
  SUBMITTED: { color: "text-slate-400", dot: "bg-slate-400" },
  VALIDATED: { color: "text-slate-300", dot: "bg-slate-300" },
  QUEUED: { color: "text-amber-400", dot: "bg-amber-500" },
  ASSIGNED: { color: "text-blue-400", dot: "bg-blue-400" },
  PROVISIONING: { color: "text-blue-300", dot: "bg-blue-300" },
  RUNNING: { color: "text-blue-400", dot: "bg-blue-500" },
  POST_PROCESSING: { color: "text-indigo-400", dot: "bg-indigo-500" },
  COMPLETED: { color: "text-emerald-400", dot: "bg-emerald-500" },
  FAILED: { color: "text-red-400", dot: "bg-red-500" },
  TIMED_OUT: { color: "text-orange-400", dot: "bg-orange-500" },
  CANCELLED: { color: "text-slate-500", dot: "bg-slate-500" },
  DEAD_LETTERED: { color: "text-red-300", dot: "bg-red-400" },
}

interface StatusBadgeProps {
  state: TaskState
  className?: string
}

export function StatusBadge({ state, className }: StatusBadgeProps) {
  const config = STATE_CONFIG[state]
  return (
    <Badge variant="outline" className={cn("gap-1.5 font-mono text-xs", config.color, className)}>
      <span className={cn("inline-block h-1.5 w-1.5 rounded-full", config.dot)} />
      {state}
    </Badge>
  )
}
