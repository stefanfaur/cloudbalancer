import { cn } from "@/lib/utils"

interface ConnectionIndicatorProps {
  isConnected: boolean
  isReconnecting: boolean
}

export function ConnectionIndicator({ isConnected, isReconnecting }: ConnectionIndicatorProps) {
  return (
    <div className="flex items-center gap-2 text-xs">
      <div
        className={cn(
          "h-2 w-2 rounded-full",
          isConnected && "bg-emerald-500",
          isReconnecting && "bg-amber-500 animate-pulse",
          !isConnected && !isReconnecting && "bg-red-500",
        )}
      />
      <span className="text-slate-400">
        {isConnected ? "Live" : isReconnecting ? "Reconnecting" : "Disconnected"}
      </span>
    </div>
  )
}
