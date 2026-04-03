import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest"
import { server } from "@/test-mocks/server"
import App from "@/App"

beforeAll(() => server.listen({ onUnhandledRequest: "bypass" }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

async function loginAs(username: string, password: string) {
  const user = userEvent.setup()
  render(<App />)

  await waitFor(() => {
    expect(screen.getByPlaceholderText(/username/i)).toBeInTheDocument()
  })

  await user.type(screen.getByPlaceholderText(/username/i), username)
  await user.type(screen.getByPlaceholderText(/password/i), password)
  await user.click(screen.getByRole("button", { name: /sign in/i }))

  await waitFor(() => {
    expect(screen.getByText("Cluster Overview")).toBeInTheDocument()
  }, { timeout: 5000 })
}

describe("Role-based visibility", () => {
  it("ADMIN sees Settings nav link", async () => {
    await loginAs("admin", "admin")
    expect(screen.getByText("Settings")).toBeInTheDocument()
  })

  it("VIEWER does not see Settings nav link", async () => {
    await loginAs("viewer", "viewer")
    expect(screen.queryByText("Settings")).not.toBeInTheDocument()
  })
})
