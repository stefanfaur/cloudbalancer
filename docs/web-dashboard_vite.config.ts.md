# File: web-dashboard/vite.config.ts

## Overview

The `web-dashboard/vite.config.ts` file serves as the central configuration module for the Vite build system used in the web-dashboard project. It defines the build pipeline, plugin integration, path resolution strategies, and the testing environment configuration for Vitest.

## Public API

This file exports a configuration object via `defineConfig` from Vite, which is consumed by the Vite CLI during development and production build processes.

- **`plugins`**: Configures the build environment with:
    - `@vitejs/plugin-react`: Enables React support (Fast Refresh).
    - `@tailwindcss/vite`: Integrates Tailwind CSS utility-first styling.
- **`resolve.alias`**: Maps the `@` symbol to the `./src` directory, facilitating cleaner import paths throughout the application.
- **`test`**: Configures the Vitest runner:
    - `globals`: Enables global test functions (e.g., `describe`, `it`, `expect`).
    - `environment`: Sets the testing environment to `jsdom` to simulate browser behavior.
    - `setupFiles`: Points to `./src/test-setup.ts` for global test initialization.
    - `exclude`: Defines patterns to ignore during test execution, specifically targeting end-to-end (e2e) tests and `node_modules`.

## Dependencies

- **`path`**: Node.js built-in module for handling file paths.
- **`vite`**: The core build tool and dev server.
- **`@vitejs/plugin-react`**: Official Vite plugin for React.
- **`@tailwindcss/vite`**: Vite plugin for Tailwind CSS integration.
- **`vitest`**: Integrated test runner (via `/// <reference types="vitest/config" />`).

## Usage Notes

- **Path Aliasing**: When importing components or utilities from the source directory, use the `@/` prefix (e.g., `import Button from "@/components/Button"`).
- **Testing**: The configuration assumes a `jsdom` environment. Ensure that any browser-specific APIs not present in `jsdom` are mocked in the `src/test-setup.ts` file.
- **Tailwind CSS**: As this configuration uses `@tailwindcss/vite`, ensure that your CSS files are correctly importing Tailwind directives to allow the plugin to process styles during the build.
- **Maintenance**: Primary maintenance for this configuration is handled by **sfaur**. Any changes to the build pipeline or test environment should be coordinated through this maintainer.