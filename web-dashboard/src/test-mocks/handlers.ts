import { http, HttpResponse } from "msw"

// Fake JWT that decodes to { sub: "admin", role: "ADMIN", exp: 9999999999 }
const ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImV4cCI6OTk5OTk5OTk5OX0.fake"
const VIEWER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2aWV3ZXIiLCJyb2xlIjoiVklFV0VSIiwiZXhwIjo5OTk5OTk5OTk5fQ.fake"
const REFRESH_TOKEN = "refresh-token-mock"

const API = "http://localhost:8080"
const METRICS = "http://localhost:8081"

export const handlers = [
  // Auth
  http.post(`${API}/api/auth/login`, async ({ request }) => {
    const body = await request.json() as { username: string; password: string }
    if (body.username === "admin" && body.password === "admin") {
      return HttpResponse.json({ accessToken: ADMIN_TOKEN, refreshToken: REFRESH_TOKEN, expiresIn: 3600 })
    }
    if (body.username === "viewer" && body.password === "viewer") {
      return HttpResponse.json({ accessToken: VIEWER_TOKEN, refreshToken: REFRESH_TOKEN, expiresIn: 3600 })
    }
    return new HttpResponse(null, { status: 401 })
  }),

  http.post(`${API}/api/auth/refresh`, () => {
    return HttpResponse.json({ accessToken: ADMIN_TOKEN, refreshToken: REFRESH_TOKEN, expiresIn: 3600 })
  }),

  http.post(`${API}/api/auth/logout`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // Tasks
  http.get(`${API}/api/tasks`, () => {
    return HttpResponse.json({
      tasks: [
        {
          id: "task-001",
          descriptor: { executorType: "SHELL", executionSpec: { command: "echo hello" }, priority: "NORMAL" },
          submittedAt: new Date().toISOString(),
          state: "COMPLETED",
          executionHistory: [{ attemptNumber: 1, workerId: "worker-001", startedAt: new Date().toISOString(), completedAt: new Date().toISOString(), exitCode: 0, workerCausedFailure: false, executionId: "e1" }],
        },
        {
          id: "task-002",
          descriptor: { executorType: "DOCKER", executionSpec: { image: "alpine" }, priority: "HIGH" },
          submittedAt: new Date().toISOString(),
          state: "RUNNING",
          executionHistory: [{ attemptNumber: 1, workerId: "worker-002", startedAt: new Date().toISOString(), exitCode: 0, workerCausedFailure: false, executionId: "e2" }],
        },
      ],
      total: 2,
      offset: 0,
      limit: 50,
    })
  }),

  http.post(`${API}/api/tasks`, () => {
    return HttpResponse.json({
      id: "task-new-001",
      descriptor: { executorType: "SIMULATED", executionSpec: { durationMs: 5000 }, priority: "NORMAL" },
      submittedAt: new Date().toISOString(),
      state: "SUBMITTED",
      executionHistory: [],
    })
  }),

  // Cluster metrics
  http.get(`${METRICS}/api/metrics/cluster`, () => {
    return HttpResponse.json({
      avgCpuPercent: 42.5,
      totalActiveTaskCount: 8,
      totalHeapUsedMB: 1536,
      throughputPerMinute: 6.2,
      avgQueueWaitMs: 120,
      avgExecutionDurationMs: 2500,
      workerCount: 3,
      healthyWorkerCount: 3,
    })
  }),

  // Worker snapshots
  http.get(`${METRICS}/api/metrics/workers`, () => {
    return HttpResponse.json([
      { workerId: "worker-001", cpuUsagePercent: 35, heapUsedMB: 400, heapMaxMB: 1024, threadCount: 15, activeTaskCount: 2, completedTaskCount: 50, failedTaskCount: 1, avgExecutionDurationMs: 2000, reportedAt: new Date().toISOString() },
      { workerId: "worker-002", cpuUsagePercent: 72, heapUsedMB: 700, heapMaxMB: 1024, threadCount: 22, activeTaskCount: 5, completedTaskCount: 150, failedTaskCount: 3, avgExecutionDurationMs: 3000, reportedAt: new Date().toISOString() },
    ])
  }),

  // Scaling
  http.get(`${API}/api/scaling/status`, () => {
    return HttpResponse.json({
      workerCount: 3,
      activeWorkerCount: 3,
      drainingWorkerCount: 0,
      policy: { minWorkers: 1, maxWorkers: 10, cooldownSeconds: 60, scaleUpStep: 1, scaleDownStep: 1, drainTimeSeconds: 30 },
      cooldownRemainingSeconds: 0,
    })
  }),

  // Strategy
  http.get(`${API}/api/admin/strategy`, () => {
    return HttpResponse.json({ strategy: "ROUND_ROBIN" })
  }),

  http.put(`${API}/api/admin/strategy`, () => {
    return HttpResponse.json({ strategy: "LEAST_CONNECTIONS" })
  }),
]
