import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import ClusterOverview from "../cluster-overview"

// Mock the API hooks
vi.mock("@/api/workers", () => ({
  useClusterMetrics: vi.fn(),
  useWorkerSnapshots: vi.fn(),
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
  AlertsProvider: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock("@/hooks/use-websocket", () => ({
  useWebSocket: () => ({ isConnected: true, isReconnecting: false }),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({ user: "admin", role: "ADMIN", accessToken: "test", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}))

// Mock recharts to avoid SVG rendering issues in jsdom
vi.mock("recharts", () => ({
  AreaChart: ({ children }: { children: React.ReactNode }) => <div data-testid="area-chart">{children}</div>,
  Area: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

import { useClusterMetrics, useWorkerSnapshots } from "@/api/workers"

const mockClusterMetrics = {
  data: {
    avgCpuPercent: 45.2,
    totalActiveTaskCount: 12,
    totalHeapUsedMB: 2048,
    throughputPerMinute: 8.5,
    avgQueueWaitMs: 150,
    avgExecutionDurationMs: 3000,
    workerCount: 4,
    healthyWorkerCount: 3,
  },
  isLoading: false,
  isError: false,
  error: null,
}

const mockWorkerSnapshots = {
  data: [
    {
      workerId: "worker-001",
      cpuUsagePercent: 55,
      heapUsedMB: 512,
      heapMaxMB: 1024,
      threadCount: 20,
      activeTaskCount: 3,
      completedTaskCount: 100,
      failedTaskCount: 2,
      avgExecutionDurationMs: 2500,
      reportedAt: "2026-04-03T10:00:00Z",
    },
    {
      workerId: "worker-002",
      cpuUsagePercent: 85,
      heapUsedMB: 768,
      heapMaxMB: 1024,
      threadCount: 25,
      activeTaskCount: 5,
      completedTaskCount: 200,
      failedTaskCount: 5,
      avgExecutionDurationMs: 3000,
      reportedAt: "2026-04-03T10:00:00Z",
    },
  ],
  isLoading: false,
  isError: false,
  error: null,
}

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        {children}
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.mocked(useClusterMetrics).mockReturnValue(mockClusterMetrics as ReturnType<typeof useClusterMetrics>)
  vi.mocked(useWorkerSnapshots).mockReturnValue(mockWorkerSnapshots as ReturnType<typeof useWorkerSnapshots>)
})

describe("ClusterOverview", () => {
  it("renders KPI cards with correct values", () => {
    render(<ClusterOverview />, { wrapper })

    expect(screen.getByText("4")).toBeInTheDocument() // workerCount
    expect(screen.getByText("12")).toBeInTheDocument() // activeTaskCount
    expect(screen.getByText("45%")).toBeInTheDocument() // avgCpuPercent
    expect(screen.getByText("8.5/min")).toBeInTheDocument() // throughput
  })

  it("renders worker cards with worker IDs", () => {
    render(<ClusterOverview />, { wrapper })

    expect(screen.getByText("worker-001")).toBeInTheDocument()
    expect(screen.getByText("worker-002")).toBeInTheDocument()
  })

  it("shows loading skeletons when data is loading", () => {
    vi.mocked(useClusterMetrics).mockReturnValue({ ...mockClusterMetrics, data: undefined, isLoading: true } as ReturnType<typeof useClusterMetrics>)
    vi.mocked(useWorkerSnapshots).mockReturnValue({ ...mockWorkerSnapshots, data: undefined, isLoading: true } as ReturnType<typeof useWorkerSnapshots>)

    render(<ClusterOverview />, { wrapper })

    // Should not have KPI values
    expect(screen.queryByText("4")).not.toBeInTheDocument()
    expect(screen.queryByText("worker-001")).not.toBeInTheDocument()
  })

  it("shows empty state when no workers", () => {
    vi.mocked(useWorkerSnapshots).mockReturnValue({ ...mockWorkerSnapshots, data: [] } as unknown as ReturnType<typeof useWorkerSnapshots>)

    render(<ClusterOverview />, { wrapper })

    expect(screen.getByText(/No workers registered/)).toBeInTheDocument()
  })
})
