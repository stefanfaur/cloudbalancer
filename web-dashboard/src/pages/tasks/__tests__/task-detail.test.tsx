import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import TaskDetail from "../task-detail"

vi.mock("@/api/tasks", () => ({
  useTask: vi.fn(),
  useTaskLogs: vi.fn(() => ({ data: null })),
  useBulkCancel: () => ({ mutateAsync: vi.fn() }),
  useBulkRetry: () => ({ mutateAsync: vi.fn() }),
}))

vi.mock("@/api/workers", () => ({
  useWorkerHistory: vi.fn(() => ({ data: null })),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: vi.fn(() => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
  AlertsProvider: ({ children }: { children: React.ReactNode }) => children,
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

import { useTask } from "@/api/tasks"
import { useAuth } from "@/hooks/use-auth"

const mockTask = {
  data: {
    id: "test-task-id-1234",
    descriptor: { executorType: "SHELL", executionSpec: { command: "echo hello" }, priority: "HIGH" },
    submittedAt: new Date().toISOString(),
    state: "RUNNING",
    executionHistory: [
      { attemptNumber: 1, workerId: "w-001", startedAt: new Date().toISOString(), exitCode: 0, workerCausedFailure: false, executionId: "e1" },
    ],
  },
  isLoading: false,
  isError: false,
  error: null,
  refetch: vi.fn(),
}

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/tasks/test-task-id-1234"]}>
        <Routes>
          <Route path="/tasks/:id" element={children} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  vi.mocked(useTask).mockReturnValue(mockTask as unknown as ReturnType<typeof useTask>)
  vi.mocked(useAuth).mockReturnValue({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
})

describe("TaskDetail", () => {
  it("renders task header with status", () => {
    render(<TaskDetail />, { wrapper })
    expect(screen.getByText("test-task-id-1234")).toBeInTheDocument()
    // RUNNING appears in both the status badge and lifecycle timeline
    const running = screen.getAllByText("RUNNING")
    expect(running.length).toBeGreaterThanOrEqual(1)
  })

  it("shows cancel button for running task", () => {
    render(<TaskDetail />, { wrapper })
    expect(screen.getByText("Cancel")).toBeInTheDocument()
  })

  it("hides action buttons for VIEWER", () => {
    vi.mocked(useAuth).mockReturnValue({ user: "viewer", role: "VIEWER", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
    render(<TaskDetail />, { wrapper })
    expect(screen.queryByText("Cancel")).not.toBeInTheDocument()
  })

  it("hides cancel button for completed task", () => {
    vi.mocked(useTask).mockReturnValue({
      ...mockTask,
      data: { ...mockTask.data, state: "COMPLETED" },
    } as unknown as ReturnType<typeof useTask>)
    render(<TaskDetail />, { wrapper })
    expect(screen.queryByText("Cancel")).not.toBeInTheDocument()
  })

  it("shows retry button for failed task", () => {
    vi.mocked(useTask).mockReturnValue({
      ...mockTask,
      data: { ...mockTask.data, state: "FAILED" },
    } as unknown as ReturnType<typeof useTask>)
    render(<TaskDetail />, { wrapper })
    expect(screen.getByText("Retry")).toBeInTheDocument()
  })
})
