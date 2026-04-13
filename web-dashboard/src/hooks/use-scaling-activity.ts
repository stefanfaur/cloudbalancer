import { useState, useEffect, useCallback, useRef } from "react"
import type { ScalingEventPayload } from "@/api/types"

const MAX_EVENTS = 50
const PENDING_TIMEOUT_MS = 30_000
const PENDING_CHECK_INTERVAL_MS = 5_000

interface PendingAction {
  action: string
  timestamp: number
}

export interface ScalingActivityState {
  events: ScalingEventPayload[]
  addPendingAction: (agentId: string, action: string) => void
  clearPendingAction: (agentId: string) => void
  isAgentPending: (agentId: string) => boolean
  getPendingAction: (agentId: string) => PendingAction | undefined
}

export function useScalingActivity(isReconnecting = false): ScalingActivityState {
  const [events, setEvents] = useState<ScalingEventPayload[]>([])
  const pendingRef = useRef<Map<string, PendingAction>>(new Map())
  const [, forceUpdate] = useState(0)
  const wasReconnecting = useRef(isReconnecting)

  // Subscribe to scaling-event custom events from WebSocket handler
  useEffect(() => {
    function onScalingEvent(e: Event) {
      const payload = (e as CustomEvent<ScalingEventPayload>).detail
      setEvents((prev) => [payload, ...prev].slice(0, MAX_EVENTS))

      // Clear matching pending action
      if (payload.agentId) {
        const pending = pendingRef.current.get(payload.agentId)
        if (pending && pending.action === payload.action) {
          pendingRef.current.delete(payload.agentId)
          forceUpdate((n) => n + 1)
        }
      }
    }

    window.addEventListener("scaling-event", onScalingEvent)
    return () => window.removeEventListener("scaling-event", onScalingEvent)
  }, [])

  // Expire stale pending actions
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now()
      let changed = false
      for (const [agentId, pending] of pendingRef.current) {
        if (now - pending.timestamp > PENDING_TIMEOUT_MS) {
          pendingRef.current.delete(agentId)
          changed = true
        }
      }
      if (changed) forceUpdate((n) => n + 1)
    }, PENDING_CHECK_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [])

  // On reconnect: clear all pending actions + append system entry
  useEffect(() => {
    if (wasReconnecting.current && !isReconnecting) {
      pendingRef.current.clear()
      setEvents((prev) => [{
        eventId: "system-reconnect",
        timestamp: new Date().toISOString(),
        action: "NONE" as const,
        triggerType: "SYSTEM",
        reason: "Reconnected — scaling state may have changed",
        previousWorkerCount: 0,
        newWorkerCount: 0,
        workersAdded: [],
        workersRemoved: [],
        agentId: null,
      }, ...prev].slice(0, MAX_EVENTS))
      forceUpdate((n) => n + 1)
    }
    wasReconnecting.current = isReconnecting
  }, [isReconnecting])

  const addPendingAction = useCallback((agentId: string, action: string) => {
    pendingRef.current.set(agentId, { action, timestamp: Date.now() })
    forceUpdate((n) => n + 1)
  }, [])

  const clearPendingAction = useCallback((agentId: string) => {
    pendingRef.current.delete(agentId)
    forceUpdate((n) => n + 1)
  }, [])

  const isAgentPending = useCallback((agentId: string) => {
    return pendingRef.current.has(agentId)
  }, [])

  const getPendingAction = useCallback((agentId: string) => {
    return pendingRef.current.get(agentId)
  }, [])

  return { events, addPendingAction, clearPendingAction, isAgentPending, getPendingAction }
}
