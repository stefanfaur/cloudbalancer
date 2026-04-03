import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { WorkerHealthState } from "@/api/types"

const STATE_CONFIG: Record<WorkerHealthState, { color: string; dot: string }> = {
  HEALTHY: { color: "text-emerald-400", dot: "bg-emerald-500" },
  SUSPECT: { color: "text-amber-400", dot: "bg-amber-500" },
  DEAD: { color: "text-red-400", dot: "bg-red-500" },
  DRAINING: { color: "text-sky-400", dot: "bg-sky-400" },
  RECOVERING: { color: "text-violet-400", dot: "bg-violet-500" },
}

interface HealthBadgeProps {
  state: WorkerHealthState
  className?: string
}

export function HealthBadge({ state, className }: HealthBadgeProps) {
  const config = STATE_CONFIG[state]
  return (
    <Badge variant="outline" className={cn("gap-1.5 font-mono text-xs", config.color, className)}>
      <span className={cn("inline-block h-1.5 w-1.5 rounded-full", config.dot)} />
      {state}
    </Badge>
  )
}
