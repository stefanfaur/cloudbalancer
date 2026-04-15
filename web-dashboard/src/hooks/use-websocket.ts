import { useEffect, useRef, useState, useCallback } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { useAuth } from "./use-auth"
import { useAlerts } from "./use-alerts"
import type { WsMessage } from "@/api/types"

const WS_URL = import.meta.env.VITE_WS_URL ?? "ws://localhost"
const MAX_BACKOFF = 30_000
const DEBOUNCE_MS = 500

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuth()
  const { addAlert } = useAlerts()
  const queryClient = useQueryClient()
  const wsRef = useRef<WebSocket | null>(null)
  const backoffRef = useRef(1000)
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout>>()
  const pendingInvalidations = useRef<Set<string>>(new Set())
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout>>()
  const [isConnected, setIsConnected] = useState(false)
  const [isReconnecting, setIsReconnecting] = useState(false)

  const flushInvalidations = useCallback(() => {
    const keys = Array.from(pendingInvalidations.current)
    pendingInvalidations.current.clear()
    for (const key of keys) {
      queryClient.invalidateQueries({ queryKey: [key] })
    }
  }, [queryClient])

  const scheduleInvalidation = useCallback((...keys: string[]) => {
    for (const key of keys) {
      pendingInvalidations.current.add(key)
    }
    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
    debounceTimerRef.current = setTimeout(flushInvalidations, DEBOUNCE_MS)
  }, [flushInvalidations])

  const handleMessage = useCallback((event: MessageEvent) => {
    try {
      const msg: WsMessage = JSON.parse(event.data)
      switch (msg.type) {
        case "TASK_UPDATE":
          scheduleInvalidation("tasks", "task")
          break
        case "WORKER_UPDATE":
          scheduleInvalidation("worker-snapshots", "cluster-metrics")
          break
        case "WORKER_STATE":
          scheduleInvalidation("worker-snapshots", "agents")
          window.dispatchEvent(new CustomEvent("scaling-progress", {
            detail: { type: "WORKER_STATE", ...msg.payload }
          }))
          break
        case "SCALING_EVENT":
          scheduleInvalidation("scaling-status", "agents")
          window.dispatchEvent(new CustomEvent("scaling-progress", {
            detail: { type: "SCALING_EVENT", ...msg.payload }
          }))
          break
        case "ALERT":
          addAlert(msg.payload)
          break
        case "CONTAINER_STARTING":
        case "CONTAINER_STARTED":
        case "CONTAINER_FAILED":
        case "WORKER_REGISTERED":
        case "WORKER_STOPPED":
        case "WORKER_STOP_FAILED":
          window.dispatchEvent(new CustomEvent("scaling-progress", {
            detail: { type: msg.type, ...msg.payload }
          }))
          break
        case "INITIAL_SNAPSHOT":
          scheduleInvalidation("worker-snapshots", "cluster-metrics", "tasks")
          break
      }
    } catch {
      // ignore malformed messages
    }
  }, [scheduleInvalidation, addAlert])

  useEffect(() => {
    if (!isAuthenticated || !accessToken) return

    function connect() {
      const ws = new WebSocket(`${WS_URL}/api/ws/events?token=${accessToken}`)

      ws.onopen = () => {
        setIsConnected(true)
        setIsReconnecting(false)
        backoffRef.current = 1000
      }

      ws.onmessage = handleMessage

      ws.onclose = () => {
        setIsConnected(false)
        wsRef.current = null
        setIsReconnecting(true)
        const delay = Math.min(backoffRef.current, MAX_BACKOFF)
        backoffRef.current = delay * 2
        reconnectTimerRef.current = setTimeout(connect, delay)
      }

      ws.onerror = () => {
        ws.close()
      }

      wsRef.current = ws
    }

    connect()

    return () => {
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current)
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
      if (wsRef.current) {
        wsRef.current.onclose = null
        wsRef.current.close()
        wsRef.current = null
      }
      setIsConnected(false)
      setIsReconnecting(false)
    }
  }, [isAuthenticated, accessToken, handleMessage])

  return { isConnected, isReconnecting }
}
