# File: web-dashboard/src/components/ui/dialog.tsx

## Overview

The `dialog.tsx` file provides a suite of accessible, modular components for creating modal dialogs (overlays) within the web dashboard. It is built upon `@base-ui/react/dialog`, ensuring robust accessibility standards (WAI-ARIA compliance), focus trapping, and keyboard navigation.

**Note:** This file is a **HOTSPOT** in the codebase. It exhibits high change frequency and structural complexity. Modifications to this file can impact multiple critical pages (e.g., task management, settings), so exercise caution and ensure comprehensive testing when updating styles or logic.

## Public API

The component library exports the following primitives:

*   **`Dialog`**: The root provider component that manages the open/closed state.
*   **`DialogTrigger`**: The element that toggles the dialog visibility.
*   **`DialogPortal`**: Renders the dialog content into the `document.body` to avoid z-index nesting issues.
*   **`DialogContent`**: The main container for the modal. Includes an optional close button.
*   **`DialogHeader` / `DialogFooter`**: Layout containers for structuring the modal content.
*   **`DialogTitle` / `DialogDescription`**: Semantic components for accessibility, providing screen readers with context about the modal.
*   **`DialogClose`**: A button or element that triggers the closing of the dialog.
*   **`DialogOverlay`**: The backdrop element that dims the background content.

## Dependencies

*   **`@base-ui/react/dialog`**: Provides the underlying logic for state management, focus trapping, and accessibility.
*   **`@/lib/utils`**: Provides the `cn` utility for merging Tailwind CSS classes.
*   **`@/components/ui/button`**: Used for consistent styling of action buttons within the dialog.
*   **`lucide-react`**: Provides the `XIcon` for the close button.

## Usage Notes

### Basic Implementation
To create a standard dialog, wrap your trigger and content within the `Dialog` root:

```tsx
import { 
  Dialog, DialogTrigger, DialogContent, DialogHeader, 
  DialogTitle, DialogDescription 
} from "@/components/ui/dialog";

function MyComponent() {
  return (
    <Dialog>
      <DialogTrigger>Open Modal</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Are you sure?</DialogTitle>
          <DialogDescription>This action cannot be undone.</DialogDescription>
        </DialogHeader>
        {/* Modal body content here */}
      </DialogContent>
    </Dialog>
  );
}
```

### Advanced Usage: Footer Actions
Use `DialogFooter` to align buttons correctly at the bottom of the modal.

```tsx
<DialogFooter>
  <DialogClose asChild>
    <Button variant="ghost">Cancel</Button>
  </DialogClose>
  <Button onClick={handleConfirm}>Confirm</Button>
</DialogFooter>
```

### Implementation Pitfalls
1.  **Z-Index Conflicts**: Since `DialogPortal` renders outside the standard DOM hierarchy, ensure that global styles or other fixed elements do not conflict with the `z-50` index defined in the `DialogContent` and `DialogOverlay` components.
2.  **Accessibility**: Always provide a `DialogTitle` and `DialogDescription` within the `DialogContent`. The `DialogDescription` is crucial for screen reader users to understand the purpose of the modal.
3.  **Hotspot Risk**: Because this component is used in high-traffic areas (Task Details, Settings), any change to the default padding or animation classes in `DialogContent` will propagate globally. Verify visual consistency across all pages after making changes.