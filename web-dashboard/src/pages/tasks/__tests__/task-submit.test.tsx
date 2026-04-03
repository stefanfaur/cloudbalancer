import { render, screen, fireEvent } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { describe, it, expect, vi, beforeEach } from "vitest"
import TaskSubmit from "../task-submit"

vi.mock("@/api/tasks", () => ({
  useSubmitTask: () => ({ mutateAsync: vi.fn().mockResolvedValue({ id: "new-task-id" }), isPending: false }),
}))

vi.mock("@/hooks/use-auth", () => ({
  useAuth: () => ({ user: "admin", role: "ADMIN", accessToken: "t", isAuthenticated: true, isLoading: false, login: vi.fn(), logout: vi.fn() }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock("@/hooks/use-alerts", () => ({
  useAlerts: () => ({ alerts: [], addAlert: vi.fn(), dismiss: vi.fn(), clearAll: vi.fn() }),
  AlertsProvider: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}))

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

describe("TaskSubmit", () => {
  beforeEach(() => {
    try { localStorage.clear() } catch { /* jsdom may not support */ }
  })

  it("renders executor type buttons", () => {
    render(<TaskSubmit />, { wrapper })
    expect(screen.getByText("SIMULATED")).toBeInTheDocument()
    expect(screen.getByText("SHELL")).toBeInTheDocument()
    expect(screen.getByText("DOCKER")).toBeInTheDocument()
    expect(screen.getByText("PYTHON")).toBeInTheDocument()
  })

  it("shows valid JSON message on validate", () => {
    render(<TaskSubmit />, { wrapper })
    fireEvent.click(screen.getByText("Validate"))
    expect(screen.getByText("Valid JSON")).toBeInTheDocument()
  })

  it("shows error on invalid JSON", () => {
    render(<TaskSubmit />, { wrapper })
    // Modify textarea to invalid JSON
    const textarea = document.querySelector("textarea")!
    fireEvent.change(textarea, { target: { value: "{ invalid" } })
    fireEvent.click(screen.getByText("Validate"))
    expect(screen.queryByText("Valid JSON")).not.toBeInTheDocument()
  })

  it("template changes on executor type click", () => {
    render(<TaskSubmit />, { wrapper })
    fireEvent.click(screen.getByText("DOCKER"))
    const textarea = document.querySelector("textarea")!
    expect(textarea.value).toContain("DOCKER")
    expect(textarea.value).toContain("alpine:latest")
  })
})
