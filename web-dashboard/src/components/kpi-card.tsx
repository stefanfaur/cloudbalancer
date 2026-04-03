import { Card, CardContent } from "@/components/ui/card"
import { cn } from "@/lib/utils"
import { TrendingUp, TrendingDown, Minus, type LucideIcon } from "lucide-react"

interface KpiCardProps {
  label: string
  value: string | number
  icon: LucideIcon
  trend?: "up" | "down" | "neutral"
  className?: string
}

const TREND_ICON = {
  up: TrendingUp,
  down: TrendingDown,
  neutral: Minus,
} as const

const TREND_COLOR = {
  up: "text-emerald-400",
  down: "text-red-400",
  neutral: "text-slate-500",
} as const

export function KpiCard({ label, value, icon: Icon, trend, className }: KpiCardProps) {
  const TrendIcon = trend ? TREND_ICON[trend] : null
  return (
    <Card className={cn("bg-slate-900 border-slate-700", className)}>
      <CardContent className="flex items-center gap-4 p-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-800 text-slate-400">
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-2xl font-semibold tabular-nums">{value}</p>
          <p className="text-xs text-slate-400 truncate">{label}</p>
        </div>
        {TrendIcon && (
          <TrendIcon className={cn("h-4 w-4", TREND_COLOR[trend!])} />
        )}
      </CardContent>
    </Card>
  )
}
