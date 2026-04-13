import { useState, useRef, useEffect } from "react"
import { ChevronDown, ChevronUp } from "lucide-react"
import type { ScalingEventPayload } from "@/api/types"

function formatTime(iso: string) {
  const d = new Date(iso)
  return d.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
}

function ActionBadge({ action }: { action: string }) {
  if (action === "SCALE_UP") {
    return <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-500/20 text-emerald-400">SCALE UP</span>
  }
  if (action === "SCALE_DOWN") {
    return <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-amber-500/20 text-amber-400">SCALE DOWN</span>
  }
  return <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-500/20 text-slate-400">{action}</span>
}

function TriggerPill({ triggerType }: { triggerType: string }) {
  return <span className="text-[10px] text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded">{triggerType}</span>
}

function WorkerIds({ ids }: { ids: string[] }) {
  if (ids.length === 0) return null
  const display = ids.length > 3 ? ids.slice(0, 3).join(", ") + ` +${ids.length - 3}` : ids.join(", ")
  return <span className="font-mono text-[10px] text-slate-500 truncate max-w-[200px] inline-block align-bottom" title={ids.join(", ")}>{display}</span>
}

function EventRow({ event }: { event: ScalingEventPayload }) {
  const isSystem = event.eventId === "system-reconnect"

  if (isSystem) {
    return (
      <div className="px-3 py-1.5 border-b border-slate-700/50 bg-slate-800/50 text-[11px] text-slate-500 italic">
        {event.reason}
      </div>
    )
  }

  return (
    <div className="px-3 py-1.5 border-b border-slate-700/50 flex items-center gap-2 text-xs">
      <span className="font-mono text-[11px] text-slate-400 shrink-0">{formatTime(event.timestamp)}</span>
      <ActionBadge action={event.action} />
      <TriggerPill triggerType={event.triggerType} />
      {event.agentId && <span className="font-mono text-[11px] text-slate-400">{event.agentId}</span>}
      <span className="text-slate-300 text-[11px]">{event.previousWorkerCount} → {event.newWorkerCount}</span>
      <WorkerIds ids={[...event.workersAdded, ...event.workersRemoved]} />
    </div>
  )
}

export function ScalingActivityLog({ events }: { events: ScalingEventPayload[] }) {
  const [isOpen, setIsOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const prevLengthRef = useRef(events.length)

  // Track unread events when panel is collapsed
  useEffect(() => {
    if (!isOpen && events.length > prevLengthRef.current) {
      setUnreadCount((c) => c + (events.length - prevLengthRef.current))
    }
    prevLengthRef.current = events.length
  }, [events.length, isOpen])

  function handleToggle() {
    setIsOpen((prev) => {
      if (!prev) setUnreadCount(0)
      return !prev
    })
  }

  return (
    <div>
      <button
        onClick={handleToggle}
        className="flex items-center gap-2 text-sm font-medium text-slate-300 hover:text-slate-100 transition-colors py-1"
      >
        Scaling Activity
        {unreadCount > 0 && (
          <span className="inline-flex items-center justify-center h-4 min-w-[16px] px-1 rounded-full bg-blue-500 text-[10px] font-bold text-white">
            {unreadCount}
          </span>
        )}
        {isOpen ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
      </button>
      {isOpen && (
        <div className="max-h-64 overflow-y-auto border border-slate-700 rounded-md bg-slate-900/50">
          {events.length === 0 ? (
            <div className="px-3 py-6 text-center text-xs text-slate-500">No scaling events yet</div>
          ) : (
            events.map((event) => <EventRow key={event.eventId} event={event} />)
          )}
        </div>
      )}
    </div>
  )
}
