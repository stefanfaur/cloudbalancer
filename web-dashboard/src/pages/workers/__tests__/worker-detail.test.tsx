import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import WorkerDetail from "../worker-detail"

vi.mock("@/api/workers", () => ({
  useWorkerSnapshots: vi.fn(),
  useWorkerHistory: vi.fn(() => ({ data: null })),
}))

vi.mock("@/api/tasks", () => ({
  useTasks: vi.fn(() => ({ data: null, isLoading: false })),
}))

vi.mock("@/api/admin", () => ({
  useUpdateWorkerTags: () => ({ mutateAsync: vi.fn() }),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: vi.fn(() => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })),
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
}))

vi.mock("recharts", () => ({
  LineChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Line: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

import { useWorkerSnapshots } from "@/api/workers"
import { useAuth } from "@/hooks/use-auth"

const mockWorkers = {
  data: [
    { workerId: "worker-alpha", cpuUsagePercent: 45, heapUsedMB: 512, heapMaxMB: 1024, threadCount: 20, activeTaskCount: 3, completedTaskCount: 100, failedTaskCount: 2, avgExecutionDurationMs: 2500, reportedAt: "2026-04-03T10:00:00Z" },
  ],
  isLoading: false,
}

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/workers/worker-alpha"]}>
        <Routes><Route path="/workers/:id" element={children} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.mocked(useWorkerSnapshots).mockReturnValue(mockWorkers as ReturnType<typeof useWorkerSnapshots>)
  vi.mocked(useAuth).mockReturnValue({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
})

describe("WorkerDetail", () => {
  it("renders worker header with health badge", () => {
    render(<WorkerDetail />, { wrapper })
    expect(screen.getByText("worker-alpha")).toBeInTheDocument()
    expect(screen.getByText("HEALTHY")).toBeInTheDocument()
  })

  it("shows tag editor for ADMIN", () => {
    render(<WorkerDetail />, { wrapper })
    expect(screen.getByPlaceholderText("Add tag...")).toBeInTheDocument()
  })

  it("hides tag editor for VIEWER", () => {
    vi.mocked(useAuth).mockReturnValue({ user: "viewer", role: "VIEWER", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
    render(<WorkerDetail />, { wrapper })
    expect(screen.queryByPlaceholderText("Add tag...")).not.toBeInTheDocument()
  })
})
