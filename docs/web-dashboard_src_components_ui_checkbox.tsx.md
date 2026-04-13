# File: web-dashboard/src/components/ui/checkbox.tsx

## Overview

The `Checkbox` component is a highly customizable, accessible form element built upon the `@base-ui/react/checkbox` primitive. It provides a consistent, styled checkbox interface for the web dashboard, featuring built-in support for focus states, error states (via `aria-invalid`), and disabled styling. The component includes a visual indicator using the `lucide-react` `CheckIcon`.

## Public API

### `Checkbox`

A functional component that renders a checkbox input. It accepts all standard properties defined by `@base-ui/react/checkbox`'s `Root` component, along with an optional `className` for styling overrides.

| Prop | Type | Description |
| :--- | :--- | :--- |
| `className` | `string` | Optional CSS classes to apply to the checkbox container. |
| `...props` | `CheckboxPrimitive.Root.Props` | Standard HTML/Base UI checkbox attributes (e.g., `checked`, `onCheckedChange`, `disabled`). |

## Dependencies

- **`@base-ui/react/checkbox`**: Provides the underlying accessible checkbox logic and state management.
- **`lucide-react`**: Provides the `CheckIcon` used as the visual indicator when the checkbox is checked.
- **`@/lib/utils`**: Provides the `cn` utility function for merging Tailwind CSS classes.

## Usage Notes

- **Styling**: The component uses Tailwind CSS for styling. It includes specific states for `data-checked`, `disabled`, and `aria-invalid` to ensure visual consistency with the application's design system.
- **Accessibility**: As it is built on `@base-ui/react`, it maintains full accessibility compliance, including keyboard navigation and screen reader support.
- **Integration**: This component is intended to be used within form structures. It is currently utilized in `web-dashboard/src/pages/tasks/task-list.tsx`.
- **Customization**: You can pass additional classes via the `className` prop to adjust the layout or spacing without overriding the core functional styles.