# Module: web-dashboard

## Overview

The `web-dashboard` module is a comprehensive React-based administrative interface designed for monitoring and managing cluster operations, task lifecycles, and worker node health. It provides a robust, real-time management layer that integrates with backend services via RESTful APIs and WebSocket streams.

The architecture is built for scalability and maintainability, utilizing **React Query** for server-state management, **Tailwind CSS** for styling, and a modular component library based on `@base-ui/react` primitives. The dashboard serves as the central control plane for operators to perform administrative actions such as task scheduling, worker scaling, and cluster configuration.

## Public API Summary

The module exposes a wide range of hooks and utilities categorized by domain:

### Data Fetching & Mutations (React Query Hooks)
*   **Tasks**: `useTasks`, `useTask`, `useSubmitTask`, `useTaskLogs`, and bulk operations (`useBulkCancel`, `useBulkRetry`, `useBulkReprioritize`).
*   **Workers & Cluster**: `useClusterMetrics`, `useWorkerHistory`, `useWorkerSnapshots`, and `useKillWorker`.
*   **Admin & Scaling**: `useAgentTokens`, `useStrategy`, `useUpdateScalingPolicy`, and `useWorkerRegistry`.
*   **Agents**: `useAgents` for real-time resource monitoring.

### Authentication & State
*   **Auth**: `useAuth` (context provider), `login`, `logout`, and `refresh` functions.
*   **Alerts**: `useAlerts` for managing global system notifications.
*   **WebSockets**: `useWebSocket` for real-time event streaming and synchronization.

### UI Components
*   **Core Primitives**: A suite of accessible components including `Button`, `Card`, `Dialog`, `Table`, `Select`, `DropdownMenu`, and `Input`.
*   **Specialized Widgets**: `CpuGauge`, `HealthBadge`, `StatusBadge`, `LifecycleTimeline`, and `ConnectionIndicator`.
*   **Layouts**: `DashboardLayout` provides the primary responsive shell for the application.

## Architecture Notes

*   **State Management**: The application strictly separates server state (managed by `react-query`) from client-side UI state (managed by React Context and local hooks).
*   **Networking Layer**: Centralized in `api/client.ts`, providing an authenticated `apiFetch` wrapper that handles global error interception and token refresh logic.
*   **Schema Consistency**: `api/types.ts` acts as the single source of truth for all data structures, ensuring type safety across the frontend and alignment with backend API contracts.
*   **Testing Strategy**: The module employs a robust testing infrastructure using **Vitest** and **Mock Service Worker (MSW)**. Network requests are intercepted at the service level, allowing for reliable integration testing of complex flows like authentication and task management without requiring a live backend.
*   **Styling**: Uses Tailwind CSS with `class-variance-authority` (CVA) to maintain a consistent design system while ensuring high reusability of UI components.
*   **Real-time Updates**: The `useWebSocket` hook is designed to map incoming server events to React Query cache invalidations, ensuring the UI remains synchronized with the cluster state without manual polling.