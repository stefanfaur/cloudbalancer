# File: web-dashboard/src/components/ui/textarea.tsx

## Overview

The `Textarea` component is a reusable, styled input element designed for multi-line text entry within the `web-dashboard` project. It leverages Tailwind CSS for consistent styling and integrates with the project's utility system to handle dynamic class merging. The component is built on top of the native HTML `textarea` element, ensuring full compatibility with standard React form handling and accessibility attributes.

## Public API

### `Textarea`

A functional React component that renders a styled `textarea` element.

**Props:**
*   `...props`: Inherits all standard attributes from `React.ComponentProps<"textarea">` (e.g., `value`, `onChange`, `placeholder`, `disabled`, `aria-invalid`).
*   `className` (optional): A string of additional CSS classes to override or extend the default component styling.

## Dependencies

*   **React**: Used for component definition and prop handling.
*   **`@/lib/utils`**: Imports the `cn` utility function, which is used to merge the base component styles with any user-provided `className`.

## Usage Notes

*   **Styling**: The component includes built-in support for `field-sizing-content`, allowing the textarea to automatically adjust its height based on content if supported by the browser.
*   **States**: The component includes specific styles for:
    *   **Focus**: Uses `focus-visible` to highlight the border and ring.
    *   **Disabled**: Applies specific opacity and background changes.
    *   **Validation**: Uses `aria-invalid` to visually indicate error states with destructive colors.
*   **Dark Mode**: Includes specific overrides for `dark` mode, ensuring the background and border colors maintain appropriate contrast.
*   **Implementation**: When using this component, spread the standard HTML textarea props into it to maintain full functionality:

```tsx
<Textarea 
  placeholder="Enter your message..." 
  onChange={(e) => console.log(e.target.value)} 
/>
```