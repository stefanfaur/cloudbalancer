import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, it, expect, vi } from "vitest"
import Analytics from "../analytics"

vi.mock("@/api/admin", () => ({
  useStrategy: vi.fn(() => ({ data: { strategy: "ROUND_ROBIN" }, isLoading: false })),
}))

vi.mock("@/api/workers", () => ({
  useClusterMetrics: vi.fn(() => ({
    data: { avgCpuPercent: 50, totalActiveTaskCount: 10, totalHeapUsedMB: 2048, throughputPerMinute: 5.0, avgQueueWaitMs: 100, avgExecutionDurationMs: 2000, workerCount: 3, healthyWorkerCount: 3 },
  })),
  useWorkerSnapshots: vi.fn(() => ({ data: [{ workerId: "w1" }, { workerId: "w2" }], isLoading: false })),
  useWorkerHistory: vi.fn(() => ({ data: [] })),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() }),
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
}))

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}><MemoryRouter>{children}</MemoryRouter></QueryClientProvider>
}

describe("Analytics", () => {
  it("renders strategy section with current strategy", () => {
    render(<Analytics />, { wrapper })
    expect(screen.getByText("ROUND_ROBIN")).toBeInTheDocument()
  })

  it("renders heatmap with worker rows", () => {
    render(<Analytics />, { wrapper })
    expect(screen.getByText("w1")).toBeInTheDocument()
    expect(screen.getByText("w2")).toBeInTheDocument()
  })

  it("cost simulator calculates correctly", () => {
    render(<Analytics />, { wrapper })
    // Default $0.10/hr, 3 workers → $0.30/hr, $7.20/day
    expect(screen.getByText("$0.30")).toBeInTheDocument()
    expect(screen.getByText("$7.20")).toBeInTheDocument()
  })
})
