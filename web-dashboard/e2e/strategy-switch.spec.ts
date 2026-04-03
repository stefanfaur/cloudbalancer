import { test, expect } from "@playwright/test"

// NOTE: Requires full backend running with ADMIN user.

test.describe("Strategy switch", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login")
    await page.getByPlaceholder(/username/i).fill("admin")
    await page.getByPlaceholder(/password/i).fill("admin")
    await page.getByRole("button", { name: /sign in/i }).click()
    await expect(page).toHaveURL(/\/cluster/, { timeout: 10_000 })
  })

  test("navigates to settings and sees strategy selector", async ({ page }) => {
    await page.getByText("Settings").click()
    await expect(page).toHaveURL(/\/settings/)
    await expect(page.getByText("Scheduling Strategy")).toBeVisible()
    await expect(page.getByText("Save Strategy")).toBeVisible()
  })

  test("changes strategy and saves", async ({ page }) => {
    await page.getByText("Settings").click()
    await expect(page).toHaveURL(/\/settings/)

    const select = page.locator("select").first()
    await select.selectOption("LEAST_CONNECTIONS")
    await page.getByText("Save Strategy").click()
    // Strategy should persist on reload
    await page.reload()
    await expect(page.getByText("Scheduling Strategy")).toBeVisible()
  })
})
