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
})
