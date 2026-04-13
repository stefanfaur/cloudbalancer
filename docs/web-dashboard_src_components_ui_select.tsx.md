# File: web-dashboard/src/components/ui/select.tsx

## Overview

`web-dashboard/src/components/ui/select.tsx` is a core UI component library file providing a highly customizable, accessible Select input implementation. It is built upon the `@base-ui/react/select` primitive, ensuring robust keyboard navigation, screen reader compatibility, and focus management.

**Warning: Hotspot File**
This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for bugs. Changes to this file impact the global dashboard navigation and data entry forms. Exercise extreme caution when modifying styles or structural logic.

## Public API

The component exports a suite of sub-components that follow the composition pattern:

*   **`Select`**: The root provider component (aliased from `SelectPrimitive.Root`).
*   **`SelectTrigger`**: The button that toggles the select menu. Supports `size` variants (`sm`, `default`).
*   **`SelectValue`**: Displays the currently selected option.
*   **`SelectContent`**: The container for the dropdown list. Handles positioning, portals, and animations.
*   **`SelectGroup`**: Used to group related `SelectItem` components.
*   **`SelectLabel`**: A non-interactive label for a `SelectGroup`.
*   **`SelectItem`**: An individual selectable option.
*   **`SelectSeparator`**: A visual divider for grouping items.
*   **`SelectScrollUpButton` / `SelectScrollDownButton`**: Indicators for scrollable content.

## Dependencies

*   **`@base-ui/react/select`**: Provides the underlying state management, accessibility logic, and primitive behaviors.
*   **`lucide-react`**: Provides iconography (`ChevronDownIcon`, `CheckIcon`, `ChevronUpIcon`).
*   **`@/lib/utils`**: Provides the `cn` utility for Tailwind class merging.

## Usage Notes

### Implementation Rationale
The component uses `data-slot` attributes to facilitate styling via CSS selectors and ensures that the component remains theme-aware (supporting `dark` mode overrides). The `SelectContent` component is wrapped in a `Portal` to ensure the dropdown renders outside of parent containers with `overflow: hidden` or `z-index` constraints.

### Potential Pitfalls
1.  **Portal Positioning**: Because `SelectContent` uses a portal, global styles affecting the body or specific container z-indices may cause the dropdown to appear behind other elements.
2.  **Size Variants**: The `SelectTrigger` includes custom logic for `size="sm"` and `size="default"`. Ensure any custom overrides to `className` do not conflict with these data-attribute-based styles.
3.  **Accessibility**: Always ensure `SelectValue` is present within the `SelectTrigger` to provide proper ARIA labeling.

### Usage Example

```tsx
import { 
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue 
} from "@/components/ui/select";

function MyComponent() {
  return (
    <Select>
      <SelectTrigger className="w-[180px]">
        <SelectValue placeholder="Select a fruit" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="apple">Apple</SelectItem>
        <SelectItem value="banana">Banana</SelectItem>
        <SelectItem value="blueberry">Blueberry</SelectItem>
      </SelectContent>
    </Select>
  );
}
```

### Styling Customization
The component relies on Tailwind CSS. When extending styles, use the `cn()` utility to ensure proper merging of classes. Avoid hardcoding `z-index` values; rely on the existing `z-50` and `isolate` classes provided in the `SelectContent` implementation to maintain consistent layering across the dashboard.