# File: web-dashboard/src/pages/tasks/task-submit.tsx

## Overview

`web-dashboard/src/pages/tasks/task-submit.tsx` is a critical UI component responsible for the manual submission of tasks to the system. It provides a JSON-based editor interface that allows users to define task execution parameters, validate them against the system's schema, and save frequently used configurations as local templates.

**Note:** This file is a **HOTSPOT**. It ranks in the top 25% for both change frequency and complexity. It is a high-risk area for bugs due to its direct interaction with `localStorage` and raw JSON input parsing.

## Public API

### Interfaces
*   **`SavedTemplate`**: Defines the structure for stored task configurations.
    *   `name`: string (User-defined label)
    *   `executorType`: `ExecutorType` (The target environment)
    *   `json`: string (Serialized task descriptor)
    *   `createdAt`: string (ISO timestamp)

### Functions
*   **`loadTemplates()`**: Retrieves the array of `SavedTemplate` objects from `localStorage`. Returns an empty array if parsing fails.
*   **`saveTemplates(templates: SavedTemplate[])`**: Persists the provided array of templates to `localStorage` under the key `cb-templates`.
*   **`handleTypeChange(type: ExecutorType)`**: Updates the editor state and resets the JSON content to the default template for the selected executor.
*   **`handleValidate()`**: Attempts to parse the current editor content as JSON. Updates the UI with success or error feedback.
*   **`handleSubmit()`**: Triggers the `useSubmitTask` mutation. On success, redirects the user to the task detail view.
*   **`handleSaveTemplate()`**: Persists the current editor state as a new template in `localStorage`.
*   **`handleLoadTemplate(template: SavedTemplate)`**: Populates the editor with the content of a previously saved template.

## Dependencies

*   **Internal APIs**: `web-dashboard/src/api/tasks.ts` (for `useSubmitTask`), `web-dashboard/src/api/types.ts` (for `ExecutorType`).
*   **UI Components**: Custom `Button` and `Card` components from `web-dashboard/src/components/ui/`.
*   **External Libraries**: `react-router-dom` (navigation), `lucide-react` (icons), `sonner` (toast notifications).

## Usage Notes

### JSON Editor Workflow
1.  **Select Executor**: Choose between `SIMULATED`, `SHELL`, `DOCKER`, or `PYTHON`. The editor will auto-populate with a base template.
2.  **Edit**: Modify the JSON content directly in the text area. The editor includes a line-number gutter for easier debugging of syntax errors.
3.  **Validate**: Click the "Validate" button before submission to ensure the JSON structure is valid.
4.  **Submit**: Click "Submit" to send the task to the backend. If successful, a toast notification will appear with a link to the new task.

### Template Management
*   **Saving**: Use the "Save as Template" button to store complex configurations. This is useful for repetitive tasks (e.g., specific Docker image configurations).
*   **Persistence**: Templates are stored in the browser's `localStorage`. Clearing browser data will remove all saved templates.
*   **Edge Cases**: 
    *   If `localStorage` is full or disabled, `saveTemplates` may fail silently or throw errors.
    *   The `handleSaveTemplate` function currently does not check for duplicate names; it simply appends to the existing list.

### Troubleshooting
*   **Validation Errors**: If the "Validate" button shows an error, check for trailing commas or unquoted keys, which are common pitfalls in manual JSON editing.
*   **Submission Failures**: Ensure the `executorType` matches the structure of the JSON provided. The backend expects specific fields based on the chosen executor.