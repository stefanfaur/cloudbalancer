import { cn } from "@/lib/utils"

interface CpuGaugeProps {
  percent: number
  size?: number
  className?: string
}

function gaugeColor(pct: number) {
  if (pct >= 80) return "stroke-red-500"
  if (pct >= 60) return "stroke-amber-500"
  return "stroke-emerald-500"
}

export function CpuGauge({ percent, size = 48, className }: CpuGaugeProps) {
  const strokeWidth = 4
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (Math.min(percent, 100) / 100) * circumference

  return (
    <div className={cn("relative inline-flex items-center justify-center", className)} style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-slate-700"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className={cn("transition-all duration-500", gaugeColor(percent))}
        />
      </svg>
      <span className="absolute text-[10px] font-mono font-medium tabular-nums">
        {Math.round(percent)}%
      </span>
    </div>
  )
}
