import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import TaskList from "../task-list"

vi.mock("@/api/tasks", () => ({
  useTasks: vi.fn(),
  useBulkCancel: () => ({ mutateAsync: vi.fn() }),
  useBulkRetry: () => ({ mutateAsync: vi.fn() }),
  useBulkReprioritize: () => ({ mutateAsync: vi.fn() }),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: vi.fn(() => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
  AlertsProvider: ({ children }: { children: React.ReactNode }) => children,
}))

import { useTasks } from "@/api/tasks"
import { useAuth } from "@/hooks/use-auth"

const mockTasks = {
  data: {
    tasks: [
      {
        id: "aaaa-bbbb-cccc-dddd",
        descriptor: { executorType: "SHELL", executionSpec: {}, priority: "HIGH" },
        submittedAt: new Date().toISOString(),
        state: "RUNNING",
        executionHistory: [{ attemptNumber: 1, workerId: "w-001", startedAt: new Date().toISOString(), exitCode: 0, workerCausedFailure: false, executionId: "e1" }],
      },
      {
        id: "eeee-ffff-1111-2222",
        descriptor: { executorType: "DOCKER", executionSpec: {}, priority: "NORMAL" },
        submittedAt: new Date().toISOString(),
        state: "COMPLETED",
        executionHistory: [],
      },
    ],
    total: 2,
    offset: 0,
    limit: 20,
  },
  isLoading: false,
  isError: false,
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
  vi.mocked(useTasks).mockReturnValue(mockTasks as ReturnType<typeof useTasks>)
  vi.mocked(useAuth).mockReturnValue({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
})

describe("TaskList", () => {
  it("renders table with mock tasks", () => {
    render(<TaskList />, { wrapper })
    expect(screen.getByText("aaaa-bbb")).toBeInTheDocument()
    expect(screen.getByText("eeee-fff")).toBeInTheDocument()
  })

  it("renders status badges", () => {
    render(<TaskList />, { wrapper })
    // Use getAllByText since status names appear in both badges and filter options
    const running = screen.getAllByText("RUNNING")
    expect(running.length).toBeGreaterThanOrEqual(1)
    const completed = screen.getAllByText("COMPLETED")
    expect(completed.length).toBeGreaterThanOrEqual(1)
  })

  it("shows submit button for ADMIN role", () => {
    render(<TaskList />, { wrapper })
    expect(screen.getByText("Submit Task")).toBeInTheDocument()
  })

  it("hides submit button for VIEWER role", () => {
    vi.mocked(useAuth).mockReturnValue({ user: "viewer", role: "VIEWER", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() })
    render(<TaskList />, { wrapper })
    expect(screen.queryByText("Submit Task")).not.toBeInTheDocument()
  })

  it("shows loading skeletons when loading", () => {
    vi.mocked(useTasks).mockReturnValue({ ...mockTasks, data: undefined, isLoading: true } as ReturnType<typeof useTasks>)
    render(<TaskList />, { wrapper })
    expect(screen.queryByText("aaaa-bbb")).not.toBeInTheDocument()
  })
})
