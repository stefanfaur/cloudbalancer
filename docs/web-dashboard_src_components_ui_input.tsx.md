# File: web-dashboard/src/components/ui/input.tsx

## Overview

The `Input` component is a reusable, styled form input element built upon the `@base-ui/react/input` primitive. It is designed to provide a consistent look and feel across the web dashboard, featuring built-in support for validation states, disabled styling, and responsive sizing.

## Public API

### `Input`

A functional React component that wraps the base input primitive with custom Tailwind CSS utility classes.

**Signature:**
```typescript
function Input({ className, type, ...props }: React.ComponentProps<"input">)
```

**Props:**
- `className` (optional): Additional CSS classes to override or extend the default component styling.
- `type` (optional): The HTML input type (e.g., "text", "password", "email").
- `...props`: All standard HTML `<input>` element attributes are supported (e.g., `value`, `onChange`, `placeholder`, `disabled`, `aria-invalid`).

## Dependencies

- **React**: Core library for component definition.
- **@base-ui/react/input**: Provides the underlying accessible input primitive.
- **@/lib/utils**: Provides the `cn` utility function for merging Tailwind class names.

## Usage Notes

- **Styling**: The component includes default styles for focus states (`focus-visible:ring-ring/50`), error states (`aria-invalid`), and disabled states. Use the `className` prop to adjust layout-specific properties like width or margin.
- **Accessibility**: The component is built on `@base-ui`, ensuring standard accessibility patterns. When indicating an error, ensure the `aria-invalid` attribute is set to `true` to trigger the built-in destructive styling.
- **Integration**: This component is used throughout the application for user input, including in the login, settings, and analytics pages.

**Example:**
```tsx
import { Input } from "@/components/ui/input";

function MyForm() {
  return (
    <Input 
      type="email" 
      placeholder="Enter your email" 
      className="w-64" 
    />
  );
}
```