# File: web-dashboard/src/test-setup.ts

## Overview

`web-dashboard/src/test-setup.ts` serves as the global configuration and initialization file for the testing environment within the `web-dashboard` project. It ensures that the testing framework is correctly extended with custom matchers and utilities required for testing UI components.

## Public API

This file does not export any functions, classes, or variables. Its execution is intended to trigger side effects during the test runner's initialization phase.

## Dependencies

- `@testing-library/jest-dom/vitest`: Extends Vitest with custom matchers for asserting the state of DOM elements (e.g., `.toBeInTheDocument()`, `.toHaveClass()`).

## Usage Notes

- **Initialization**: This file should be referenced in the Vitest configuration file (typically `vitest.config.ts` or `vitest.workspace.ts`) under the `setupFiles` property.
- **Side Effects**: By importing `@testing-library/jest-dom/vitest`, this file globally registers matchers to the `expect` object provided by Vitest.
- **Maintenance**: As this is a global setup file, any additional global test mocks, environment polyfills, or test-wide configuration should be added here to ensure they are applied before any test suites execute.