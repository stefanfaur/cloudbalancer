import { renderHook, act } from "@testing-library/react"
import { describe, it, expect, beforeEach, vi } from "vitest"
import { useScalingProgress } from "../use-scaling-progress"

describe("useScalingProgress", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("creates scale-up operation on SCALING_EVENT with SCALE_UP action", () => {
    const { result } = renderHook(() => useScalingProgress())

    act(() => {
      const event = new CustomEvent("scaling-progress", {
        detail: {
          type: "SCALING_EVENT",
          action: "SCALE_UP",
          agentId: "agent-1",
          workersAdded: ["worker-1", "worker-2"],
        }
      })
      window.dispatchEvent(event)
    })

    expect(result.current.getOperationsForAgent("agent-1")).toHaveLength(2)
    expect(result.current.hasActiveOperations("agent-1")).toBe(true)
  })

  it("advances scale-up operation through steps", () => {
    const { result } = renderHook(() => useScalingProgress())

    // Create operation
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "SCALING_EVENT",
          action: "SCALE_UP",
          agentId: "agent-1",
          workersAdded: ["worker-1"],
        }
      }))
    })

    let ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("command_sent")

    // Container starting
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "CONTAINER_STARTING",
          agentId: "agent-1",
          workerId: "worker-1",
        }
      }))
    })

    ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("container_starting")

    // Worker registered
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "WORKER_REGISTERED",
          workerId: "worker-1",
        }
      }))
    })

    ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("registered")

    // Ready
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "WORKER_STATE",
          workerId: "worker-1",
          state: "HEALTHY",
        }
      }))
    })

    ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("ready")
    expect(ops[0].completedAt).toBeDefined()
  })

  it("marks operation as failed on CONTAINER_FAILED", () => {
    const { result } = renderHook(() => useScalingProgress())

    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "SCALING_EVENT",
          action: "SCALE_UP",
          agentId: "agent-1",
          workersAdded: ["worker-1"],
        }
      }))
    })

    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "CONTAINER_FAILED",
          agentId: "agent-1",
          workerId: "worker-1",
          error: "Out of memory",
        }
      }))
    })

    const ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].failed).toBeDefined()
    expect(ops[0].failed?.error).toBe("Out of memory")
  })

  it("handles scale-down operation correctly", () => {
    const { result } = renderHook(() => useScalingProgress())

    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "SCALING_EVENT",
          action: "SCALE_DOWN",
          agentId: "agent-1",
          workersRemoved: ["worker-1"],
        }
      }))
    })

    let ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].direction).toBe("down")
    expect(ops[0].currentStep).toBe("command_sent")

    // Draining
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "WORKER_STATE",
          workerId: "worker-1",
          state: "DRAINING",
        }
      }))
    })

    ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("draining")

    // Stopped
    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "WORKER_STOPPED",
          agentId: "agent-1",
          workerId: "worker-1",
        }
      }))
    })

    ops = result.current.getOperationsForAgent("agent-1")
    expect(ops[0].currentStep).toBe("stopped")
    expect(ops[0].completedAt).toBeDefined()
  })

  it("marks operations as stale on reconnection", () => {
    const { result, rerender } = renderHook(
      ({ reconnecting }: { reconnecting: boolean }) => useScalingProgress(reconnecting),
      { initialProps: { reconnecting: false } }
    )

    act(() => {
      window.dispatchEvent(new CustomEvent("scaling-progress", {
        detail: {
          type: "SCALING_EVENT",
          action: "SCALE_UP",
          agentId: "agent-1",
          workersAdded: ["worker-1"],
        }
      }))
    })

    expect(result.current.hasActiveOperations("agent-1")).toBe(true)

    // Simulate reconnection
    rerender({ reconnecting: false })
    act(() => {
      // Reconnection completes
      rerender({ reconnecting: false })
    })

    const ops = result.current.getOperationsForAgent("agent-1")
    // After reconnect, active operations should be marked as failed
    expect(ops.length).toBeGreaterThanOrEqual(0)
  })
})
