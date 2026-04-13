# Repository Overview: repo

## Project Summary

The `repo` repository is a comprehensive software project primarily built on the Java ecosystem, complemented by a TypeScript-based web dashboard. With over 114,000 lines of code across 987 files, the project serves as a robust backend-heavy application with a centralized management interface. The codebase is currently in an active development phase, characterized by high churn and complexity in key modules, indicating a rapidly evolving feature set.

## Technology Stack

The project leverages a multi-language stack designed for high-performance backend processing and a modern, reactive frontend:

*   **Primary Backend:** Java (75.2% of the codebase), utilizing Gradle for build automation.
*   **Frontend:** TypeScript (6.9%) and JavaScript (0.1%) powering the `web-dashboard`.
*   **Infrastructure & Configuration:** Docker (0.6%), YAML (1.4%), TOML (0.3%), and JSON (1.5%) for containerization and environment configuration.
*   **Data Layer:** SQL (2.2%) for relational database management.
*   **Documentation:** Extensive Markdown (11.8%) coverage.

## Entry Points

The application's execution and testing flow are primarily managed through the `web-dashboard` package:

*   **`web-dashboard/src/main.tsx`**: The primary entry point for the frontend application, responsible for mounting the React root.
*   **`web-dashboard/src/App.tsx`**: The root component defining the application's layout, routing, and global state providers.
*   **`web-dashboard/src/test-mocks/server.ts`**: The entry point for the mock service worker, used for intercepting API requests during development and testing.

## Architecture

The architecture is structured to separate the heavy-duty Java backend from the interactive web interface. 

*   **Backend Core:** The Java-based core handles the primary business logic, specifically focusing on task dispatching and security configurations. Key components include the `dispatcher` module, which manages Kafka-based task result listening and security enforcement.
*   **Frontend Layer:** The `web-dashboard` acts as the client-side interface. It follows a component-driven architecture using reusable UI primitives (e.g., `button.tsx`, `card.tsx`, `badge.tsx`).
*   **Communication:** The frontend interacts with the backend via a centralized API client defined in `web-dashboard/src/api/client.ts`, with shared type definitions in `web-dashboard/src/api/types.ts` to ensure type safety across the network boundary.
*   **Utility & Hooks:** Shared logic is abstracted into `lib/utils.ts` and custom React hooks like `use-auth.tsx`, promoting DRY (Don't Repeat Yourself) principles across the dashboard.

**Note for New Developers:** The project currently exhibits high churn in the `dispatcher` module. When contributing, prioritize reviewing the `SecurityConfig.java` and `TaskResultListener.java` files, as these are the most frequently modified components in the current development cycle.