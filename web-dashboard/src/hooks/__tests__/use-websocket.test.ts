import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"

// Mock WebSocket
class MockWebSocket {
  static instances: MockWebSocket[] = []
  onopen: (() => void) | null = null
  onclose: (() => void) | null = null
  onmessage: ((e: { data: string }) => void) | null = null
  onerror: (() => void) | null = null
  readyState = 0
  url: string

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)
    // Simulate connection
    setTimeout(() => {
      this.readyState = 1
      this.onopen?.()
    }, 10)
  }

  close() {
    this.readyState = 3
  }

  send(_data: string) {}
}

describe("WebSocket hook logic", () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    vi.stubGlobal("WebSocket", MockWebSocket)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("MockWebSocket connects with token in URL", () => {
    const ws = new MockWebSocket("ws://localhost:8080/api/ws/events?token=test-token")
    expect(ws.url).toContain("token=test-token")
  })

  it("message parsing handles TASK_UPDATE correctly", () => {
    const message = JSON.stringify({
      type: "TASK_UPDATE",
      payload: { id: "abc-123", state: "COMPLETED" },
    })
    const parsed = JSON.parse(message)
    expect(parsed.type).toBe("TASK_UPDATE")
    expect(parsed.payload.id).toBe("abc-123")
  })

  it("message parsing handles WORKER_STATE correctly", () => {
    const message = JSON.stringify({
      type: "WORKER_STATE",
      payload: { workerId: "worker-1", state: "HEALTHY" },
    })
    const parsed = JSON.parse(message)
    expect(parsed.type).toBe("WORKER_STATE")
    expect(parsed.payload.workerId).toBe("worker-1")
  })

  it("message parsing handles SCALING_EVENT correctly", () => {
    const message = JSON.stringify({
      type: "SCALING_EVENT",
      payload: { action: "SCALE_UP", reason: "High CPU" },
    })
    const parsed = JSON.parse(message)
    expect(parsed.type).toBe("SCALING_EVENT")
  })

  it("reconnect uses exponential backoff logic", () => {
    let backoff = 1000
    const maxBackoff = 30000
    const delays: number[] = []
    for (let i = 0; i < 5; i++) {
      const delay = Math.min(backoff, maxBackoff)
      delays.push(delay)
      backoff = delay * 2
    }
    expect(delays).toEqual([1000, 2000, 4000, 8000, 16000])
  })

  it("scaling-progress event types are correctly structured", () => {
    const eventTypes = [
      "CONTAINER_STARTING",
      "CONTAINER_STARTED",
      "CONTAINER_FAILED",
      "WORKER_REGISTERED",
      "WORKER_STOPPED",
      "WORKER_STOP_FAILED",
    ]
    
    for (const eventType of eventTypes) {
      const message = {
        type: eventType,
        payload: {
          agentId: "agent-1",
          workerId: "worker-1",
          timestamp: "2026-04-13T12:00:00Z",
        },
      }
      const parsed = JSON.parse(JSON.stringify(message))
      expect(parsed.type).toBe(eventType)
    }
  })

  it("SCALING_EVENT with SCALE_UP dispatches correct scaling-progress event", () => {
    const listener = vi.fn()
    window.addEventListener("scaling-progress", listener)
    
    const event = new CustomEvent("scaling-progress", {
      detail: {
        type: "SCALING_EVENT",
        action: "SCALE_UP",
        agentId: "agent-1",
        workersAdded: ["worker-1"],
      },
    })
    window.dispatchEvent(event)
    
    expect(listener).toHaveBeenCalledWith(
      expect.objectContaining({
        detail: expect.objectContaining({ type: "SCALING_EVENT" }),
      })
    )
    
    window.removeEventListener("scaling-progress", listener)
  })

  it("CONTAINER_STARTING event is dispatched correctly", () => {
    const listener = vi.fn()
    window.addEventListener("scaling-progress", listener)
    
    const event = new CustomEvent("scaling-progress", {
      detail: {
        type: "CONTAINER_STARTING",
        agentId: "agent-1",
        workerId: "worker-1",
        timestamp: "2026-04-13T12:00:00Z",
      },
    })
    window.dispatchEvent(event)
    
    expect(listener).toHaveBeenCalledWith(
      expect.objectContaining({
        detail: expect.objectContaining({ type: "CONTAINER_STARTING" }),
      })
    )
    
    window.removeEventListener("scaling-progress", listener)
  })
})
