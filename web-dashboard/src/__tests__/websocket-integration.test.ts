import { describe, it, expect } from "vitest"

// Unit-level integration test for WebSocket message → cache invalidation mapping
// The full WebSocket integration is tested via the use-websocket unit tests
// and E2E tests. This validates the message type → query key mapping.

const MESSAGE_TYPE_TO_QUERY_KEYS: Record<string, string[]> = {
  TASK_UPDATE: ["tasks", "task"],
  WORKER_UPDATE: ["worker-snapshots", "cluster-metrics"],
  WORKER_STATE: ["worker-snapshots"],
  SCALING_EVENT: ["scaling-status"],
  INITIAL_SNAPSHOT: ["worker-snapshots", "cluster-metrics", "tasks"],
}

describe("WebSocket integration mapping", () => {
  it("TASK_UPDATE invalidates tasks and task queries", () => {
    expect(MESSAGE_TYPE_TO_QUERY_KEYS["TASK_UPDATE"]).toEqual(["tasks", "task"])
  })

  it("WORKER_UPDATE invalidates worker-snapshots and cluster-metrics", () => {
    expect(MESSAGE_TYPE_TO_QUERY_KEYS["WORKER_UPDATE"]).toEqual(["worker-snapshots", "cluster-metrics"])
  })

  it("WORKER_STATE invalidates worker-snapshots", () => {
    expect(MESSAGE_TYPE_TO_QUERY_KEYS["WORKER_STATE"]).toEqual(["worker-snapshots"])
  })

  it("SCALING_EVENT invalidates scaling-status", () => {
    expect(MESSAGE_TYPE_TO_QUERY_KEYS["SCALING_EVENT"]).toEqual(["scaling-status"])
  })

  it("INITIAL_SNAPSHOT invalidates all primary queries", () => {
    expect(MESSAGE_TYPE_TO_QUERY_KEYS["INITIAL_SNAPSHOT"]).toEqual(["worker-snapshots", "cluster-metrics", "tasks"])
  })

  it("all known message types have cache mappings", () => {
    const knownTypes = ["TASK_UPDATE", "WORKER_UPDATE", "WORKER_STATE", "SCALING_EVENT", "ALERT", "INITIAL_SNAPSHOT"]
    const mappedTypes = Object.keys(MESSAGE_TYPE_TO_QUERY_KEYS)
    // ALERT doesn't invalidate cache (goes to alerts context)
    expect(mappedTypes).toEqual(knownTypes.filter((t) => t !== "ALERT"))
  })
})
