# File: web-dashboard/src/components/ui/button.tsx

## Overview

The `web-dashboard/src/components/ui/button.tsx` file provides a highly reusable, accessible `Button` component built upon the `@base-ui/react/button` primitive. It leverages `class-variance-authority` (CVA) to manage complex Tailwind CSS class variations, ensuring consistent styling across the application.

This component is the standard building block for all interactive buttons within the dashboard, supporting various visual styles and sizes while maintaining accessibility standards (such as focus rings and ARIA state handling).

## Public API

### `Button`
A React component that renders an interactive button. It accepts all standard HTML button attributes in addition to custom variant props.

**Props:**
*   `variant` (optional): Defines the visual style of the button.
    *   `default` (primary action)
    *   `outline` (bordered background)
    *   `secondary` (secondary action)
    *   `ghost` (transparent background)
    *   `destructive` (danger/delete action)
    *   `link` (text-only link style)
*   `size` (optional): Defines the dimensions and padding.
    *   `default`, `xs`, `sm`, `lg`
    *   `icon`, `icon-xs`, `icon-sm`, `icon-lg` (square dimensions for icon-only buttons)
*   `...props`: All standard `ButtonPrimitive.Props` (e.g., `onClick`, `disabled`, `type`).

### `buttonVariants`
A CVA configuration object exported for use in other components that may need to replicate the button's styling (e.g., links styled as buttons).

## Dependencies

*   **`@base-ui/react/button`**: Provides the underlying accessible button primitive.
*   **`class-variance-authority`**: Used to manage the mapping of variants and sizes to Tailwind CSS classes.
*   **`@/lib/utils`**: Provides the `cn` utility function for merging and resolving Tailwind class names.

## Usage Notes

*   **Tailwind Integration**: The component uses `group/button` and specific data attributes (e.g., `data-slot="button"`) to allow for complex styling interactions, such as icon sizing or group-based layout adjustments.
*   **Icons**: The component includes built-in logic to handle SVG icons. It automatically resizes icons based on the chosen button size using the `[&_svg:not([class*='size-'])]:size-x` selector pattern.
*   **Accessibility**: The component is built on `Base UI`, ensuring proper keyboard navigation, focus management, and ARIA attribute support out of the box.
*   **Example**:
    ```tsx
    import { Button } from "@/components/ui/button";

    <Button variant="destructive" size="sm" onClick={handleDelete}>
      Delete Item
    </Button>
    ```