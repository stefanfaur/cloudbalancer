# File: web-dashboard/src/pages/settings.tsx

## Overview

`web-dashboard/src/pages/settings.tsx` is the central administrative hub for the web dashboard. It provides a unified interface for managing system-wide configurations, including scheduling strategies, auto-scaling policies, agent authentication, and chaos engineering experiments.

**⚠️ HOTSPOT WARNING:** This file is a high-activity component with significant complexity. It integrates multiple critical API layers and manages sensitive system state. Changes to this file carry a high risk of introducing regressions in cluster stability or security. Exercise extreme caution when modifying state management or API mutation logic.

## Public API

The page is structured into functional sections, each exposing specific handlers for administrative actions:

### Scheduling Strategy
- `handleSave()`: Persists the selected scheduling strategy (e.g., `ROUND_ROBIN`, `CUSTOM`) and associated weights to the backend.

### Auto-Scaling Policy
- `updateField(key, value)`: Updates local form state for scaling parameters (e.g., `minWorkers`, `cooldownSeconds`).
- `handleSave()`: Validates scaling constraints (e.g., `minWorkers < maxWorkers`) and pushes the new policy to the scaling engine.
- `handleTrigger()`: Executes manual scaling overrides (Scale Up/Down) based on user-defined counts.

### Agent Tokens
- `handleCreate()`: Generates a new registration token for worker agents.
- `handleCopy(text)`: Utility to copy the generated token to the system clipboard.
- `handleRevoke()`: Permanently invalidates an existing agent token.

### Chaos Monkey
- `handleConfirm()`: Executes destructive operations (killing workers, failing tasks, or injecting latency) for testing system resilience.

## Dependencies

This component relies on several internal API hooks and UI primitives:

- **API Layers**:
  - `web-dashboard/src/api/admin.ts`: Manages agent tokens, worker lifecycle, and chaos injection.
  - `web-dashboard/src/api/scaling.ts`: Interfaces with the auto-scaling engine for policy updates and manual triggers.
- **UI Components**: Uses a custom library of Radix-based components (`Button`, `Card`, `Input`, `Label`, `Dialog`) located in `web-dashboard/src/components/ui/`.
- **Utilities**: `sonner` for toast notifications and `lucide-react` for iconography.

## Usage Notes

### Managing Scaling Policies
When updating scaling policies, the system performs client-side validation. 
1. Ensure `minWorkers` is strictly less than `maxWorkers`.
2. All numeric inputs must be positive integers.
3. **Pitfall**: The `handleSave` function does not automatically sanitize input types beyond basic parsing; ensure the UI state remains synchronized with the expected API schema.

### Agent Token Lifecycle
- **Creation**: Tokens are displayed only once upon creation. Users must copy the token immediately using the provided clipboard utility.
- **Revocation**: Revoking a token is an irreversible action. Any agents currently using the revoked token will be unable to re-register or authenticate.

### Chaos Engineering (High Risk)
The `ChaosMonkeySection` is designed for testing system resilience. 
- **Caution**: Actions triggered here are destructive. Killing workers or failing tasks in a production environment will result in immediate service degradation.
- **Confirmation**: All actions require a secondary confirmation dialog to prevent accidental triggers.

### Example: Custom Scheduling Weights
To configure a custom scheduling strategy:
1. Select **CUSTOM** from the strategy dropdown.
2. Adjust the weights for `cpu`, `memory`, `taskCount`, and `latency` in the expanded input fields.
3. Click **Save Strategy**. The system will now prioritize nodes based on these weighted factors.