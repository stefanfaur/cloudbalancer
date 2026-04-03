import { cn } from "@/lib/utils"
import { formatDistanceToNow } from "date-fns"
import type { ExecutionAttempt, TaskState } from "@/api/types"

interface LifecycleTimelineProps {
  executionHistory: ExecutionAttempt[]
  currentState: TaskState
}

const LIFECYCLE_ORDER: TaskState[] = [
  "SUBMITTED",
  "VALIDATED",
  "QUEUED",
  "ASSIGNED",
  "PROVISIONING",
  "RUNNING",
  "POST_PROCESSING",
  "COMPLETED",
]

const TERMINAL_STATES: TaskState[] = ["COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED", "DEAD_LETTERED"]

function stateColor(state: TaskState, isCurrent: boolean, isPast: boolean) {
  if (isCurrent) {
    if (TERMINAL_STATES.includes(state)) {
      return state === "COMPLETED" ? "bg-emerald-500" : "bg-red-500"
    }
    return "bg-blue-500 animate-pulse"
  }
  if (isPast) return "bg-slate-500"
  return "bg-slate-700"
}

export function LifecycleTimeline({ executionHistory, currentState }: LifecycleTimelineProps) {
  const currentIdx = LIFECYCLE_ORDER.indexOf(currentState)
  const isTerminal = TERMINAL_STATES.includes(currentState)

  // Build steps: use standard lifecycle, but replace terminal state if needed
  const steps = isTerminal && currentIdx === -1
    ? [...LIFECYCLE_ORDER.slice(0, -1), currentState]
    : LIFECYCLE_ORDER

  const lastAttempt = executionHistory[executionHistory.length - 1]

  return (
    <div className="flex items-center gap-0">
      {steps.map((step, i) => {
        const isCurrent = step === currentState
        const isPast = isTerminal ? true : i < currentIdx
        const isLast = i === steps.length - 1

        return (
          <div key={step} className="flex items-center">
            <div className="flex flex-col items-center gap-1">
              <div className={cn("h-3 w-3 rounded-full", stateColor(step, isCurrent, isPast))} />
              <span className={cn(
                "text-[10px] font-mono whitespace-nowrap",
                isCurrent ? "text-slate-200" : "text-slate-500",
              )}>
                {step}
              </span>
              {isCurrent && lastAttempt?.startedAt && (
                <span className="text-[9px] text-slate-600">
                  {formatDistanceToNow(new Date(lastAttempt.startedAt), { addSuffix: true })}
                </span>
              )}
            </div>
            {!isLast && (
              <div className={cn(
                "h-0.5 w-6 mx-1",
                isPast || isCurrent ? "bg-slate-500" : "bg-slate-700",
              )} />
            )}
          </div>
        )
      })}
    </div>
  )
}
