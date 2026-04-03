import { render, screen } from "@testing-library/react"
import { HealthBadge } from "../health-badge"
import { StatusBadge } from "../status-badge"
import { describe, it, expect } from "vitest"

describe("HealthBadge", () => {
  it("HEALTHY renders with emerald color class", () => {
    render(<HealthBadge state="HEALTHY" />)
    const badge = screen.getByText("HEALTHY")
    expect(badge.closest("[class]")?.className).toMatch(/emerald/)
  })

  it("DEAD renders with red color class", () => {
    render(<HealthBadge state="DEAD" />)
    const badge = screen.getByText("DEAD")
    expect(badge.closest("[class]")?.className).toMatch(/red/)
  })

  it("SUSPECT renders with amber color class", () => {
    render(<HealthBadge state="SUSPECT" />)
    const badge = screen.getByText("SUSPECT")
    expect(badge.closest("[class]")?.className).toMatch(/amber/)
  })

  it("DRAINING renders with sky color class", () => {
    render(<HealthBadge state="DRAINING" />)
    const badge = screen.getByText("DRAINING")
    expect(badge.closest("[class]")?.className).toMatch(/sky/)
  })

  it("RECOVERING renders with violet color class", () => {
    render(<HealthBadge state="RECOVERING" />)
    const badge = screen.getByText("RECOVERING")
    expect(badge.closest("[class]")?.className).toMatch(/violet/)
  })
})

describe("StatusBadge", () => {
  it("COMPLETED renders with emerald color class", () => {
    render(<StatusBadge state="COMPLETED" />)
    const badge = screen.getByText("COMPLETED")
    expect(badge.closest("[class]")?.className).toMatch(/emerald/)
  })

  it("FAILED renders with red color class", () => {
    render(<StatusBadge state="FAILED" />)
    const badge = screen.getByText("FAILED")
    expect(badge.closest("[class]")?.className).toMatch(/red/)
  })

  it("RUNNING renders with blue color class", () => {
    render(<StatusBadge state="RUNNING" />)
    const badge = screen.getByText("RUNNING")
    expect(badge.closest("[class]")?.className).toMatch(/blue/)
  })

  it("QUEUED renders with amber color class", () => {
    render(<StatusBadge state="QUEUED" />)
    const badge = screen.getByText("QUEUED")
    expect(badge.closest("[class]")?.className).toMatch(/amber/)
  })

  it("CANCELLED renders with slate color class", () => {
    render(<StatusBadge state="CANCELLED" />)
    const badge = screen.getByText("CANCELLED")
    expect(badge.closest("[class]")?.className).toMatch(/slate/)
  })
})
