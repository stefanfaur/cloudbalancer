import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { BrowserRouter } from "react-router-dom"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { AuthProvider } from "@/hooks/use-auth"
import LoginPage from "@/pages/login"
import { http, HttpResponse } from "msw"
import { setupServer } from "msw/node"
import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest"

// Create a valid-looking JWT for testing
function fakeJwt(sub: string, role: string) {
  const header = btoa(JSON.stringify({ alg: "HS256" }))
  const payload = btoa(JSON.stringify({ sub, role, exp: Date.now() / 1000 + 900 }))
  return `${header}.${payload}.fake-sig`
}

const server = setupServer()

function renderLogin() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("LoginPage", () => {
  it("renders login form", () => {
    renderLogin()
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument()
  })

  it("shows error on invalid credentials", async () => {
    server.use(
      http.post("http://localhost:8080/api/auth/login", () => {
        return new HttpResponse(null, { status: 401 })
      })
    )
    renderLogin()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText(/username/i), "bad")
    await user.type(screen.getByLabelText(/password/i), "wrong")
    await user.click(screen.getByRole("button", { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/invalid/i)
    })
  })

  it("disables submit button while loading", async () => {
    server.use(
      http.post("http://localhost:8080/api/auth/login", async () => {
        await new Promise((r) => setTimeout(r, 500))
        return HttpResponse.json({
          accessToken: fakeJwt("admin", "ADMIN"),
          refreshToken: "rt",
          expiresIn: 900,
        })
      })
    )
    renderLogin()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText(/username/i), "admin")
    await user.type(screen.getByLabelText(/password/i), "pass")
    await user.click(screen.getByRole("button", { name: /sign in/i }))

    expect(screen.getByRole("button")).toBeDisabled()
  })
})
