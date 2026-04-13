# File: web-dashboard/src/components/ui/badge.tsx

## Overview

The `Badge` component is a versatile, reusable UI element used to display status, labels, or categories throughout the `web-dashboard`. It is built using `class-variance-authority` (CVA) for robust style management and `@base-ui/react` for flexible rendering, allowing it to adapt to various semantic contexts while maintaining a consistent design language.

## Public API

### `Badge`

A functional component that renders a badge element. It accepts standard HTML `span` attributes and variant-specific properties.

**Props:**

| Prop | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `variant` | `"default" \| "secondary" \| "destructive" \| "outline" \| "ghost" \| "link"` | `"default"` | Determines the visual style of the badge. |
| `className` | `string` | `undefined` | Additional CSS classes to apply to the badge. |
| `render` | `Function` | `undefined` | Custom render prop for advanced composition via `@base-ui/react`. |
| `...props` | `React.HTMLAttributes<HTMLSpanElement>` | - | Standard HTML span attributes. |

### `badgeVariants`

The underlying CVA configuration object used to generate the CSS classes for the component. This can be imported and used independently if you need to apply badge styles to non-badge elements.

## Dependencies

- **`@base-ui/react`**: Used for `mergeProps` and `useRender` to handle component composition and attribute merging.
- **`class-variance-authority`**: Used to manage the complex variant-based styling logic.
- **`@/lib/utils`**: Provides the `cn` utility for merging Tailwind CSS classes safely.

## Usage Notes

- **Default Behavior**: By default, the `Badge` renders as a `<span>`.
- **Styling**: The component uses Tailwind CSS with specific support for icons. If you include an icon inside the badge, it will automatically adjust padding if the icon has a `data-icon` attribute set to `inline-start` or `inline-end`.
- **Accessibility**: The component includes focus-visible states, making it suitable for interactive badges (e.g., links or buttons).
- **Composition**: Because it uses `useRender`, you can easily override the underlying tag or inject custom behavior by providing a `render` prop.

**Example Usage:**

```tsx
import { Badge } from "@/components/ui/badge";

// Basic usage
<Badge>Default Badge</Badge>

// Destructive variant
<Badge variant="destructive">Error</Badge>

// Outline variant
<Badge variant="outline">Draft</Badge>
```