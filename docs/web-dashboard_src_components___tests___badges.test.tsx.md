# File: web-dashboard/src/components/__tests__/badges.test.tsx

## Overview

`web-dashboard/src/components/__tests__/badges.test.tsx` is a test suite designed to verify the visual styling and rendering logic of the `HealthBadge` and `StatusBadge` components. The tests ensure that specific application states correctly map to their intended Tailwind CSS color classes.

## Public API

This file does not export any members; it is a test suite intended for execution by the Vitest test runner.

## Dependencies

- **@testing-library/react**: Used for rendering React components and querying the DOM during tests.
- **vitest**: The testing framework providing the `describe`, `it`, and `expect` functions.
- **../health-badge**: The component under test, responsible for displaying system health states.
- **../status-badge**: The component under test, responsible for displaying task or process status.

## Usage Notes

- **Testing Strategy**: The tests utilize `screen.getByText` to locate the badge and inspect the `className` of the closest parent element to verify that the correct color theme is applied based on the `state` prop.
- **State Mapping**:
    - `HealthBadge` states:
        - `HEALTHY`: emerald
        - `DEAD`: red
        - `SUSPECT`: amber
        - `DRAINING`: sky
        - `RECOVERING`: violet
    - `StatusBadge` states:
        - `COMPLETED`: emerald
        - `FAILED`: red
        - `RUNNING`: blue
        - `QUEUED`: amber
        - `CANCELLED`: slate
- **Execution**: Run these tests using the project's test script (typically `npm test` or `vitest`) to ensure that UI changes do not inadvertently break the color-coding logic for badges.