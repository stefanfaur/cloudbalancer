# File: web-dashboard/src/components/ui/card.tsx

## Overview

The `web-dashboard/src/components/ui/card.tsx` file provides a modular, highly reusable set of components for building consistent card-based layouts within the web dashboard. It leverages Tailwind CSS for styling and follows a composition-based pattern, allowing developers to assemble complex card structures (headers, content, actions, and footers) with minimal boilerplate.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a foundational UI component, any modifications here have a high risk of propagating visual regressions across the entire application. Exercise extreme caution when altering the base styles or data-attribute logic.

## Public API

The module exports a suite of functional components designed to be used together:

*   **`Card`**: The primary container. Supports a `size` prop (`"default"` | `"sm"`) to toggle density.
*   **`CardHeader`**: A container for titles and descriptions. Automatically handles layout adjustments if `CardAction` or `CardDescription` are present.
*   **`CardTitle`**: Styled text component for card headings.
*   **`CardDescription`**: Styled text component for secondary information.
*   **`CardAction`**: A specialized container for placing buttons or controls in the top-right corner of the header.
*   **`CardContent`**: The main body container for card content.
*   **`CardFooter`**: A distinct, styled section at the bottom of the card, typically used for buttons or status indicators.

## Dependencies

*   **`react`**: Used for component definition and prop typing.
*   **`@/lib/utils`**: Imports the `cn` utility function, which merges Tailwind classes and resolves conflicts using `clsx` and `tailwind-merge`.

## Usage Notes

### Composition Pattern
The components use `data-slot` attributes and Tailwind's `group` modifiers to handle internal state (like sizing) automatically. When you change the `size` prop on the parent `Card`, all child components adjust their padding and font sizes accordingly.

### Basic Example
```tsx
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";

function MyComponent() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Dashboard Metrics</CardTitle>
      </CardHeader>
      <CardContent>
        <p>Content goes here...</p>
      </CardContent>
    </Card>
  );
}
```

### Advanced Example with Actions and Footer
```tsx
import { Card, CardHeader, CardTitle, CardAction, CardContent, CardFooter } from "@/components/ui/card";

function ActionCard() {
  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle>Task Status</CardTitle>
        <CardAction>
          <button>Edit</button>
        </CardAction>
      </CardHeader>
      <CardContent>
        Processing...
      </CardContent>
      <CardFooter>
        <p>Last updated: 2m ago</p>
      </CardFooter>
    </Card>
  );
}
```

### Pitfalls and Best Practices
*   **Avoid Custom Overrides**: Because this is a high-risk hotspot, prefer using the provided `size` prop over passing arbitrary `className` overrides that might conflict with the internal `group` logic.
*   **Image Handling**: The `Card` component includes specific logic for `img` elements. Placing an `img` as the first child will automatically remove top padding to allow for edge-to-edge header images.
*   **Layout Constraints**: The `CardHeader` uses `@container/card-header`. Avoid adding complex layout styles to the `CardHeader` that might interfere with the internal `grid` layout defined in the component.