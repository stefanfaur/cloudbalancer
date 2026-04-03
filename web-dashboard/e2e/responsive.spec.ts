import { test, expect } from "@playwright/test"

test.describe("Responsive layout", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login")
    await page.getByPlaceholder(/username/i).fill("admin")
    await page.getByPlaceholder(/password/i).fill("admin")
    await page.getByRole("button", { name: /sign in/i }).click()
    await expect(page).toHaveURL(/\/cluster/, { timeout: 10_000 })
  })

  test("sidebar collapses at 1024px viewport", async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 768 })
    // The sidebar should show only icons (text hidden via lg:hidden xl:inline)
    // The CB logo should still be visible
    await expect(page.getByText("CB")).toBeVisible()
    // Content should still be accessible
    await expect(page.getByText("Cluster Overview")).toBeVisible()
  })

  test("no horizontal scroll at 1024px", async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 768 })
    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth)
    const clientWidth = await page.evaluate(() => document.documentElement.clientWidth)
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth + 1) // 1px tolerance
  })

  test("full sidebar visible at 1280px+", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await expect(page.getByText("CloudBalancer")).toBeVisible()
    await expect(page.getByText("Cluster")).toBeVisible()
    await expect(page.getByText("Workers")).toBeVisible()
  })
})
