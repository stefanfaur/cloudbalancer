# File: web-dashboard/src/pages/tasks/task-templates.tsx

## Overview

`web-dashboard/src/pages/tasks/task-templates.tsx` is a React component responsible for managing user-saved task configurations. It provides a dashboard interface to view, load, and delete previously saved task templates stored in the browser's `localStorage`.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for bugs due to its direct interaction with browser storage and navigation state.

## Public API

### Interfaces
*   **`SavedTemplate`**: Defines the structure of a stored task configuration.
    *   `name` (string): The user-defined label for the template.
    *   `executorType` (ExecutorType): The type of executor associated with the task.
    *   `json` (string): The serialized task configuration payload.
    *   `createdAt` (string): ISO timestamp of when the template was saved.

### Functions
*   **`loadTemplates()`**: Retrieves the array of `SavedTemplate` objects from `localStorage` under the key `cb-templates`. Returns an empty array if parsing fails.
*   **`saveTemplates(templates: SavedTemplate[])`**: Persists the provided array of templates to `localStorage`.
*   **`handleDelete(index: number)`**: Removes a template from the state and updates `localStorage`.
*   **`handleLoad(template: SavedTemplate)`**: Navigates the user to the `/tasks/submit` route, passing the selected template via the `state` object.

## Dependencies

*   **UI Components**: Uses internal project components (`Button`, `Badge`, `Table`) from `@/components/ui`.
*   **API Types**: Imports `ExecutorType` from `@/api/types`.
*   **Utilities**: 
    *   `date-fns`: Used for relative time formatting (`formatDistanceToNow`).
    *   `lucide-react`: Provides UI icons (`ArrowLeft`, `Trash2`, `Play`).
    *   `react-router-dom`: Manages navigation and routing state.

## Usage Notes

### Template Lifecycle
1.  **Creation**: Templates are created via the "Submit Task" page (not this file).
2.  **Storage**: Templates are stored in the client's browser using `localStorage`. This means templates are **not shared** across different browsers or devices.
3.  **Loading**: When a user clicks the "Play" button, the template data is passed through the React Router `state` object. The destination page (`/tasks/submit`) must be configured to read `location.state.template` to populate the form.

### Potential Pitfalls
*   **Storage Limits**: Since `localStorage` has a capacity limit (typically 5MB), storing large amounts of complex task JSONs may eventually cause `QuotaExceededError` during `saveTemplates`.
*   **Data Integrity**: Because the data is stored as a raw JSON string in `localStorage`, manual modification by the user or schema changes in `SavedTemplate` could lead to runtime errors during `loadTemplates`. Always ensure the `JSON.parse` logic is wrapped in a try-catch block.
*   **State Synchronization**: The component uses local React state (`templates`) to track the list. If other parts of the application modify `localStorage` directly, this component will not automatically reflect those changes without a refresh or state sync mechanism.

### Example: Loading a Template
To programmatically trigger a template load from another component, ensure the navigation state is structured correctly:

```typescript
// Example of how the destination page consumes the state
const location = useLocation();
const template = location.state?.template as SavedTemplate | undefined;

useEffect(() => {
  if (template) {
    // Logic to populate form fields with template.json
  }
}, [template]);
```