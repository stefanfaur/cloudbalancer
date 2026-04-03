import { useAlerts } from "@/hooks/use-alerts"
import { AlertTriangle, Info, XCircle, X } from "lucide-react"
import { cn } from "@/lib/utils"

const SEVERITY_CONFIG: Record<string, { icon: typeof AlertTriangle; color: string; bg: string }> = {
  error: { icon: XCircle, color: "text-red-400", bg: "bg-red-500/10 border-red-500/20" },
  warning: { icon: AlertTriangle, color: "text-amber-400", bg: "bg-amber-500/10 border-amber-500/20" },
  info: { icon: Info, color: "text-blue-400", bg: "bg-blue-500/10 border-blue-500/20" },
}

export function AlertsBanner() {
  const { alerts, dismiss, clearAll } = useAlerts()

  if (alerts.length === 0) return null

  const visible = alerts.slice(0, 5)

  return (
    <div className="space-y-1 mb-4">
      <div className="flex items-center justify-between px-1">
        <span className="text-xs text-slate-500">{alerts.length} alert{alerts.length !== 1 ? "s" : ""}</span>
        {alerts.length > 1 && (
          <button onClick={clearAll} className="text-xs text-slate-500 hover:text-slate-300">
            Clear all
          </button>
        )}
      </div>
      {visible.map((alert, i) => {
        const config = SEVERITY_CONFIG[alert.severity] ?? SEVERITY_CONFIG.info
        const Icon = config.icon
        return (
          <div
            key={`${alert.timestamp}-${i}`}
            className={cn("flex items-center gap-2 px-3 py-2 rounded-md border text-sm", config.bg)}
          >
            <Icon className={cn("h-4 w-4 shrink-0", config.color)} />
            <span className="flex-1 text-slate-200 truncate">{alert.message}</span>
            <button onClick={() => dismiss(i)} className="text-slate-500 hover:text-slate-300">
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )
      })}
      {alerts.length > 5 && (
        <p className="text-xs text-slate-500 px-1">+ {alerts.length - 5} more</p>
      )}
    </div>
  )
}
