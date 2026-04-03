import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest"
import { server } from "@/test-mocks/server"
import App from "@/App"

beforeAll(() => server.listen({ onUnhandledRequest: "bypass" }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("Auth flow integration", () => {
  it("redirects to login when unauthenticated", async () => {
    render(<App />)
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/username/i)).toBeInTheDocument()
    })
  })

  it("logs in with valid credentials and shows dashboard", async () => {
    const user = userEvent.setup()
    render(<App />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/username/i)).toBeInTheDocument()
    })

    await user.type(screen.getByPlaceholderText(/username/i), "admin")
    await user.type(screen.getByPlaceholderText(/password/i), "admin")
    await user.click(screen.getByRole("button", { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText("Cluster Overview")).toBeInTheDocument()
    }, { timeout: 5000 })
  })

  it("shows error on invalid credentials", async () => {
    const user = userEvent.setup()
    render(<App />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/username/i)).toBeInTheDocument()
    })

    await user.type(screen.getByPlaceholderText(/username/i), "wrong")
    await user.type(screen.getByPlaceholderText(/password/i), "wrong")
    await user.click(screen.getByRole("button", { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText(/invalid|error|failed/i)).toBeInTheDocument()
    }, { timeout: 3000 })
  })
})
