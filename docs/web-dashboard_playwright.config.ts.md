# File: web-dashboard/playwright.config.ts

## Overview

The `web-dashboard/playwright.config.ts` file serves as the central configuration module for the Playwright end-to-end (E2E) testing suite within the `web-dashboard` project. It defines the execution environment, browser settings, and local development server integration required to run automated tests against the dashboard application.

## Public API

This file exports a single default configuration object created via `defineConfig` from `@playwright/test`.

### Configuration Object Properties
- **`testDir`**: Specifies the directory containing test files (`./e2e`).
- **`timeout`**: Sets the global timeout for each test to 30,000 milliseconds (30 seconds).
- **`retries`**: Configured to `0`, meaning tests will not automatically retry upon failure.
- **`use`**: Defines default browser context settings:
    - `baseURL`: The target URL for the application under test (`http://localhost:5173`).
    - `headless`: Runs browsers in headless mode by default.
    - `screenshot`: Captures screenshots only when a test fails.
- **`projects`**: Defines the browser environments. Currently configured for `chromium`.
- **`webServer`**: Manages the lifecycle of the local development server:
    - `command`: The command used to start the server (`npm run dev`).
    - `url`: The URL to wait for before starting tests.
    - `reuseExistingServer`: Set to `true` to prevent redundant server restarts if one is already running.
    - `timeout`: The wait time for the server to become ready (15,000 milliseconds).

## Dependencies

- **`@playwright/test`**: The core testing framework package used to define the configuration schema and test runner behavior.

## Usage Notes

- **Environment Requirements**: Ensure that the `npm run dev` command is functional within the `web-dashboard` directory, as the test suite relies on this to spin up the local development server.
- **Debugging**: To view the browser during test execution, you can override the `headless` property to `false` in your local environment or via CLI arguments.
- **Extending Browsers**: To add support for other browsers (e.g., Firefox or WebKit), add new objects to the `projects` array following the existing Chromium pattern.
- **CI/CD Integration**: The `reuseExistingServer: true` setting is optimized for local development; ensure your CI pipeline environment is compatible with this configuration or override it using environment variables if necessary.