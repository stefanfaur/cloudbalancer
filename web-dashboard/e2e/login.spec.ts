import { test, expect } from "@playwright/test"

// NOTE: These E2E tests require the full backend running (dispatcher + metrics-aggregator).
// Run with: docker-compose up -d && npx playwright test

test.describe("Login flow", () => {
  test("redirects unauthenticated user to /login", async ({ page }) => {
    await page.goto("/")
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByPlaceholder(/username/i)).toBeVisible()
  })

  test("logs in and redirects to /cluster", async ({ page }) => {
    await page.goto("/login")
    await page.getByPlaceholder(/username/i).fill("admin")
    await page.getByPlaceholder(/password/i).fill("admin")
    await page.getByRole("button", { name: /sign in/i }).click()
    await expect(page).toHaveURL(/\/cluster/, { timeout: 10_000 })
    await expect(page.getByText("Cluster Overview")).toBeVisible()
  })

  test("shows error on invalid credentials", async ({ page }) => {
    await page.goto("/login")
    await page.getByPlaceholder(/username/i).fill("wrong")
    await page.getByPlaceholder(/password/i).fill("wrong")
    await page.getByRole("button", { name: /sign in/i }).click()
    await expect(page.getByText(/invalid|error|failed/i)).toBeVisible({ timeout: 5000 })
  })
})
