import { useState, useEffect, useCallback, useRef } from "react"

// Step types
type ScaleUpStep = "decision" | "command_sent" | "container_starting" | "registered" | "ready"
type ScaleDownStep = "decision" | "command_sent" | "draining" | "stopped"

// Scaling operation tracking
export interface ScalingOperation {
  direction: "up" | "down"
  agentId: string
  workerId: string
  startedAt: number
  currentStep: ScaleUpStep | ScaleDownStep
  stepTimestamps: Partial<Record<string, number>>
  failed?: { error: string; timestamp: string }
  completedAt?: number
}

// Hook return interface
export interface ScalingProgressState {
  getOperationsForAgent(agentId: string): ScalingOperation[]
  hasActiveOperations(agentId: string): boolean
}

const CLEANUP_DELAY_MS = 10_000        // fade out after step complete
const FAILED_CLEANUP_DELAY_MS = 30_000 // fade out after error
const RECONNECT_STALE_FADE_MS = 5_000  // fade out reconnect stale ops

export function useScalingProgress(isReconnecting = false): ScalingProgressState {
  const [operations, setOperations] = useState<Map<string, ScalingOperation>>(new Map())
  const cleanupTimersRef = useRef<Map<string, NodeJS.Timeout>>(new Map())
  const wasReconnectingRef = useRef(isReconnecting)

  // Helper: create operation for scale-up
  const createScaleUpOperation = useCallback((
    agentId: string,
    workerId: string,
    timestamp: number
  ): ScalingOperation => ({
    direction: "up",
    agentId,
    workerId,
    startedAt: timestamp,
    currentStep: "command_sent",  // SCALING_EVENT fires after command send
    stepTimestamps: {
      decision: timestamp,
      command_sent: timestamp,
    },
  }), [])

  // Helper: create operation for scale-down
  const createScaleDownOperation = useCallback((
    agentId: string,
    workerId: string,
    timestamp: number
  ): ScalingOperation => ({
    direction: "down",
    agentId,
    workerId,
    startedAt: timestamp,
    currentStep: "command_sent",
    stepTimestamps: {
      decision: timestamp,
      command_sent: timestamp,
    },
  }), [])

  // Schedule cleanup of completed/failed operation
  const scheduleCleanup = useCallback((operationKey: string, delayMs: number) => {
    // Clear any existing timer
    const existingTimer = cleanupTimersRef.current.get(operationKey)
    if (existingTimer) clearTimeout(existingTimer)

    // Set new timer
    const timer = setTimeout(() => {
      setOperations((prev) => {
        const next = new Map(prev)
        next.delete(operationKey)
        return next
      })
      cleanupTimersRef.current.delete(operationKey)
    }, delayMs)

    cleanupTimersRef.current.set(operationKey, timer)
  }, [])

  // Handle scaling-progress custom events
  useEffect(() => {
    function onScalingProgress(e: Event) {
      const event = (e as CustomEvent<any>).detail
      const opKey = `${event.agentId}-${event.workerId}`
      const now = Date.now()

      setOperations((prev) => {
        const next = new Map(prev)
        let op = next.get(opKey)

        switch (event.type) {
          // Scale-up: create operation
          case "SCALING_EVENT": {
            if (event.action === "SCALE_UP" && event.workersAdded) {
              for (const workerId of event.workersAdded) {
                const key = `${event.agentId}-${workerId}`
                next.set(key, createScaleUpOperation(event.agentId, workerId, now))
              }
            } else if (event.action === "SCALE_DOWN" && event.workersRemoved) {
              for (const workerId of event.workersRemoved) {
                const key = `${event.agentId}-${workerId}`
                next.set(key, createScaleDownOperation(event.agentId, workerId, now))
              }
            }
            break
          }

          // Scale-up: container starting
          case "CONTAINER_STARTING": {
            if (op) {
              op = { ...op, currentStep: "container_starting" }
              op.stepTimestamps = { ...op.stepTimestamps, container_starting: now }
              next.set(opKey, op)
            }
            break
          }

          // Scale-up: worker registered
          case "WORKER_REGISTERED": {
            if (op && op.direction === "up") {
              op = { ...op, currentStep: "registered" }
              op.stepTimestamps = { ...op.stepTimestamps, registered: now }
              next.set(opKey, op)
            }
            break
          }

          // Scale-up: worker ready (healthy state)
          case "WORKER_STATE": {
            if (op && event.state === "HEALTHY" && op.direction === "up") {
              op = { ...op, currentStep: "ready", completedAt: now }
              op.stepTimestamps = { ...op.stepTimestamps, ready: now }
              next.set(opKey, op)
              // Schedule cleanup after completion
              scheduleCleanup(opKey, CLEANUP_DELAY_MS)
            } else if (op && event.state === "DRAINING" && op.direction === "down") {
              op = { ...op, currentStep: "draining" }
              op.stepTimestamps = { ...op.stepTimestamps, draining: now }
              next.set(opKey, op)
            }
            break
          }

          // Scale-down: worker stopped
          case "WORKER_STOPPED": {
            if (op && op.direction === "down") {
              op = { ...op, currentStep: "stopped", completedAt: now }
              op.stepTimestamps = { ...op.stepTimestamps, stopped: now }
              next.set(opKey, op)
              // Schedule cleanup after completion
              scheduleCleanup(opKey, CLEANUP_DELAY_MS)
            }
            break
          }

          // Error cases
          case "CONTAINER_FAILED": {
            if (op && op.direction === "up") {
              op = {
                ...op,
                failed: { error: event.error, timestamp: event.timestamp },
              }
              next.set(opKey, op)
              scheduleCleanup(opKey, FAILED_CLEANUP_DELAY_MS)
            }
            break
          }

          case "WORKER_STOP_FAILED": {
            if (op && op.direction === "down") {
              op = {
                ...op,
                failed: { error: event.error, timestamp: event.timestamp },
              }
              next.set(opKey, op)
              scheduleCleanup(opKey, FAILED_CLEANUP_DELAY_MS)
            }
            break
          }
        }

        return next
      })
    }

    window.addEventListener("scaling-progress", onScalingProgress)
    return () => window.removeEventListener("scaling-progress", onScalingProgress)
  }, [createScaleUpOperation, createScaleDownOperation, scheduleCleanup])

  // Handle reconnection: mark all active operations as stale
  useEffect(() => {
    if (wasReconnectingRef.current && !isReconnecting) {
      setOperations((prev) => {
        const next = new Map(prev)
        for (const [key, op] of next) {
          if (!op.completedAt && !op.failed) {
            op = {
              ...op,
              failed: { error: "Connection lost — state unknown", timestamp: new Date().toISOString() },
            }
            next.set(key, op)
            scheduleCleanup(key, RECONNECT_STALE_FADE_MS)
          }
        }
        return next
      })
    }
    wasReconnectingRef.current = isReconnecting
  }, [isReconnecting, scheduleCleanup])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      for (const timer of cleanupTimersRef.current.values()) {
        clearTimeout(timer)
      }
    }
  }, [])

  const getOperationsForAgent = useCallback((agentId: string): ScalingOperation[] => {
    const result: ScalingOperation[] = []
    for (const op of operations.values()) {
      if (op.agentId === agentId) {
        result.push(op)
      }
    }
    return result
  }, [operations])

  const hasActiveOperations = useCallback((agentId: string): boolean => {
    for (const op of operations.values()) {
      if (op.agentId === agentId && !op.completedAt && !op.failed) {
        return true
      }
    }
    return false
  }, [operations])

  return { getOperationsForAgent, hasActiveOperations }
}
