# File: web-dashboard/src/components/ui/label.tsx

## Overview

The `web-dashboard/src/components/ui/label.tsx` file defines a reusable, accessible `Label` component for the web dashboard. It is designed to be used in conjunction with form inputs, providing consistent styling and behavior across the application. The component leverages Tailwind CSS for styling and integrates with the `cn` utility to handle dynamic class merging.

## Public API

### `Label`

A functional React component that renders a standard HTML `<label>` element with pre-applied styles.

- **Props**: Accepts all standard attributes of a native HTML `<label>` element (via `React.ComponentProps<"label">`).
- **Behavior**:
    - Applies a default set of styles including `text-sm`, `font-medium`, and `select-none`.
    - Automatically handles disabled states for associated inputs using the `peer-disabled` selector.
    - Supports the `group-data-[disabled=true]` state for integration with parent container disabled states.
    - Merges custom `className` props with internal styles using the `cn` utility.

## Dependencies

- `react`: Used for component definition and type definitions.
- `@/lib/utils`: Imports the `cn` utility function for Tailwind class merging.

## Usage Notes

- **Integration**: This component is intended to be used as a wrapper or companion to form inputs. When used with inputs, ensure the input has a `peer` class if you wish to utilize the automatic `peer-disabled` styling.
- **Styling**: The component uses a "data-slot" attribute (`data-slot="label"`) to facilitate easier targeting in complex component compositions.
- **Customization**: You can override or extend the default styles by passing a `className` prop. The `cn` utility ensures that your custom classes are merged correctly with the default styles without conflicts.

**Example Usage:**

```tsx
import { Label } from "@/components/ui/label";

function MyForm() {
  return (
    <div className="group">
      <Label htmlFor="email">Email Address</Label>
      <input id="email" className="peer" type="email" />
    </div>
  );
}
```