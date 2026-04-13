# File: web-dashboard/eslint.config.js

## Overview

The `web-dashboard/eslint.config.js` file serves as the centralized ESLint configuration for the web-dashboard project. It utilizes the modern ESLint "flat config" format to define linting rules, environment settings, and file exclusions for the TypeScript-based React application.

## Public API

This file exports a single configuration object via `defineConfig`, which is consumed by the ESLint engine.

*   **`globalIgnores`**: Configured to ignore the `dist` directory to prevent linting of build artifacts.
*   **`files`**: Targets all files with `.ts` and `.tsx` extensions.
*   **`languageOptions`**: Sets the ECMAScript version to `2020` and enables browser-specific global variables.

## Dependencies

The configuration relies on the following external packages:

*   **`@eslint/js`**: Provides the base recommended JavaScript linting rules.
*   **`globals`**: Supplies predefined global variable definitions for the browser environment.
*   **`eslint-plugin-react-hooks`**: Enforces rules for React Hooks.
*   **`eslint-plugin-react-refresh`**: Provides rules for React Fast Refresh integration.
*   **`typescript-eslint`**: Enables TypeScript-specific linting rules and parser integration.
*   **`eslint/config`**: Provides the `defineConfig` utility and `globalIgnores` helper.

## Usage Notes

*   **Flat Config**: This file uses the flat configuration format (`eslint.config.js`). Ensure your IDE and ESLint CLI version are compatible with this format (ESLint v9+).
*   **Extends**: The configuration aggregates rules from:
    *   `js.configs.recommended`
    *   `tseslint.configs.recommended`
    *   `reactHooks.configs.flat.recommended`
    *   `reactRefresh.configs.vite`
*   **Modifications**: To add custom project-specific rules, add a `rules` object within the configuration array for the targeted file patterns.
*   **Maintenance**: Primary maintenance is handled by **sfaur**.