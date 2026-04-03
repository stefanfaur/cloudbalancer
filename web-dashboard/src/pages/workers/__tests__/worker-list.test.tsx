import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import WorkerList from "../worker-list"

vi.mock("@/api/workers", () => ({
  useWorkerSnapshots: vi.fn(),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() }),
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
}))

import { useWorkerSnapshots } from "@/api/workers"

const mockWorkers = {
  data: [
    { workerId: "worker-alpha", cpuUsagePercent: 45, heapUsedMB: 512, heapMaxMB: 1024, threadCount: 20, activeTaskCount: 3, completedTaskCount: 100, failedTaskCount: 2, avgExecutionDurationMs: 2500, reportedAt: "2026-04-03T10:00:00Z" },
    { workerId: "worker-beta", cpuUsagePercent: 88, heapUsedMB: 900, heapMaxMB: 1024, threadCount: 30, activeTaskCount: 8, completedTaskCount: 500, failedTaskCount: 10, avgExecutionDurationMs: 1500, reportedAt: "2026-04-03T10:00:00Z" },
  ],
  isLoading: false,
}

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}><MemoryRouter>{children}</MemoryRouter></QueryClientProvider>
}

beforeEach(() => {
  vi.mocked(useWorkerSnapshots).mockReturnValue(mockWorkers as ReturnType<typeof useWorkerSnapshots>)
})

describe("WorkerList", () => {
  it("renders table with worker IDs", () => {
    render(<WorkerList />, { wrapper })
    expect(screen.getByText("worker-alpha")).toBeInTheDocument()
    expect(screen.getByText("worker-beta")).toBeInTheDocument()
  })

  it("renders health badges", () => {
    render(<WorkerList />, { wrapper })
    // Both are HEALTHY (cpu < 95%)
    const badges = screen.getAllByText("HEALTHY")
    expect(badges.length).toBe(2)
  })

  it("shows empty state when no workers", () => {
    vi.mocked(useWorkerSnapshots).mockReturnValue({ data: [], isLoading: false } as unknown as ReturnType<typeof useWorkerSnapshots>)
    render(<WorkerList />, { wrapper })
    expect(screen.getByText(/No workers registered/)).toBeInTheDocument()
  })
})
