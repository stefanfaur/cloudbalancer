# File: web-dashboard/src/components/ui/dropdown-menu.tsx

## Overview

The `dropdown-menu.tsx` file provides a comprehensive, accessible, and highly customizable suite of components for building dropdown menus in the `web-dashboard`. Built upon the `@base-ui/react/menu` primitive, it ensures robust keyboard navigation, screen reader support, and consistent styling across the application.

**IMPORTANT: HOTSPOT WARNING**
This file is a **high-activity hotspot** within the codebase, ranking in the top 25% for both change frequency and complexity. It is a critical UI dependency. Any modifications to this file carry a high risk of introducing regressions across multiple dashboard modules. Exercise extreme caution when refactoring or updating styles.

## Public API

The following components are exported for use in the dashboard:

*   **`DropdownMenu`**: The root wrapper component that manages the state of the menu.
*   **`DropdownMenuTrigger`**: The element that toggles the visibility of the menu.
*   **`DropdownMenuContent`**: The container for menu items. Handles positioning and animations.
*   **`DropdownMenuGroup`**: A wrapper for grouping related menu items.
*   **`DropdownMenuLabel`**: A non-interactive label for grouping items.
*   **`DropdownMenuItem`**: The standard interactive menu item. Supports a `variant` prop (`default` or `destructive`).
*   **`DropdownMenuCheckboxItem`**: An item that toggles a boolean state.
*   **`DropdownMenuRadioGroup` / `DropdownMenuRadioItem`**: Components for mutually exclusive selection lists.
*   **`DropdownMenuSeparator`**: A visual divider for menu items.
*   **`DropdownMenuShortcut`**: A helper component for displaying keyboard shortcuts (e.g., "⌘K").
*   **`DropdownMenuSub` / `DropdownMenuSubTrigger` / `DropdownMenuSubContent`**: Components for creating nested, multi-level dropdown menus.
*   **`DropdownMenuPortal`**: Renders the menu content in a portal to avoid z-index and overflow clipping issues.

## Dependencies

*   **`@base-ui/react/menu`**: The underlying headless UI logic provider.
*   **`lucide-react`**: Provides standard iconography (e.g., `ChevronRightIcon`, `CheckIcon`).
*   **`@/lib/utils`**: Provides the `cn` utility for Tailwind class merging.

## Usage Notes

### Implementation Rationale
The component uses Tailwind CSS for styling, leveraging `data-` attributes provided by `@base-ui` to handle state-driven styling (e.g., `data-open`, `data-disabled`, `data-variant`). This approach keeps the component logic decoupled from the visual presentation, allowing for easy theme adjustments.

### Common Pitfalls
1.  **Z-Index Issues**: Always wrap your `DropdownMenuContent` in a `DropdownMenuPortal` if the menu is being clipped by parent containers with `overflow: hidden` or `z-index` constraints.
2.  **Keyboard Navigation**: The component automatically handles focus management. Do not manually override focus behavior unless strictly necessary, as this may break accessibility compliance.
3.  **Destructive Actions**: When using `variant="destructive"` on `DropdownMenuItem`, ensure the user has a clear visual cue, as the component automatically applies specific colors to the background and text.

### Example: Standard Dropdown
```tsx
<DropdownMenu>
  <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
  <DropdownMenuPortal>
    <DropdownMenuContent>
      <DropdownMenuGroup>
        <DropdownMenuItem onClick={() => console.log("Profile")}>
          Profile
        </DropdownMenuItem>
        <DropdownMenuItem variant="destructive">
          Delete Account
        </DropdownMenuItem>
      </DropdownMenuGroup>
    </DropdownMenuContent>
  </DropdownMenuPortal>
</DropdownMenu>
```

### Example: Nested Submenu
```tsx
<DropdownMenuSub>
  <DropdownMenuSubTrigger>More Options</DropdownMenuSubTrigger>
  <DropdownMenuPortal>
    <DropdownMenuSubContent>
      <DropdownMenuItem>Sub-item 1</DropdownMenuItem>
    </DropdownMenuSubContent>
  </DropdownMenuPortal>
</DropdownMenuSub>
```