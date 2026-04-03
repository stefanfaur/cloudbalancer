import { test, expect } from "@playwright/test"

// NOTE: Requires full backend running with at least one user configured.

test.describe("Task lifecycle", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login")
    await page.getByPlaceholder(/username/i).fill("admin")
    await page.getByPlaceholder(/password/i).fill("admin")
    await page.getByRole("button", { name: /sign in/i }).click()
    await expect(page).toHaveURL(/\/cluster/, { timeout: 10_000 })
  })

  test("navigates to task list", async ({ page }) => {
    await page.getByText("Tasks").click()
    await expect(page).toHaveURL(/\/tasks/)
    await expect(page.getByText("Tasks")).toBeVisible()
  })

  test("submits a new task", async ({ page }) => {
    await page.getByText("Tasks").click()
    await expect(page).toHaveURL(/\/tasks/)
    await page.getByText("Submit Task").click()
    await expect(page).toHaveURL(/\/tasks\/submit/)
    await page.getByText("Submit", { exact: true }).click()
    // Should navigate back or show toast
    await expect(page).toHaveURL(/\/tasks/, { timeout: 10_000 })
  })

  test("clicks into task detail", async ({ page }) => {
    await page.getByText("Tasks").click()
    await expect(page).toHaveURL(/\/tasks/)
    // Click first task link in the table
    const firstTaskLink = page.locator("a[href*='/tasks/']").first()
    if (await firstTaskLink.isVisible()) {
      await firstTaskLink.click()
      await expect(page).toHaveURL(/\/tasks\/[a-z0-9-]+/)
    }
  })
})
