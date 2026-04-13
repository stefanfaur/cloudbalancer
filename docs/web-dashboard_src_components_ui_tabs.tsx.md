# File: web-dashboard/src/components/ui/tabs.tsx

## Overview

The `web-dashboard/src/components/ui/tabs.tsx` file provides a set of accessible, styled tab components built on top of `@base-ui/react/tabs`. It leverages Tailwind CSS for styling and `class-variance-authority` (CVA) to manage component variants, ensuring a consistent design language across the application.

This implementation supports both horizontal and vertical orientations and offers two visual variants: `default` (boxed/pill style) and `line` (minimalist underline style).

## Public API

The module exports the following components and utilities:

### `Tabs`
The root container for the tab system.
- **Props**: Extends `TabsPrimitive.Root.Props`.
- **Orientation**: Supports `horizontal` (default) or `vertical`.

### `TabsList`
The container for the tab triggers.
- **Props**: Extends `TabsPrimitive.List.Props` and `VariantProps<typeof tabsListVariants>`.
- **Variants**: 
    - `default`: Adds a background container (muted).
    - `line`: Removes background, suitable for tab headers with bottom-border indicators.

### `TabsTrigger`
The individual clickable tab button.
- **Props**: Extends `TabsPrimitive.Tab.Props`.
- **Features**: Includes complex state-based styling for active states, focus rings, and orientation-specific indicator positioning (via `after` pseudo-elements).

### `TabsContent`
The container for the panel content associated with a specific tab.
- **Props**: Extends `TabsPrimitive.Panel.Props`.

### `tabsListVariants`
A CVA configuration object defining the styling logic for the `TabsList` component.

## Dependencies

- **@base-ui/react/tabs**: Provides the underlying accessible tab logic and state management.
- **class-variance-authority**: Used to handle variant-based class composition.
- **@/lib/utils**: Provides the `cn` utility function for merging Tailwind classes.

## Usage Notes

- **Orientation**: To change the layout, pass the `orientation` prop to the `Tabs` component. When set to `vertical`, the `TabsList` will automatically adjust its layout to stack triggers vertically.
- **Styling**: The components use `data-slot` attributes and `group` modifiers (e.g., `group/tabs`, `group/tabs-list`) to handle complex state styling, such as active indicators and hover effects.
- **Accessibility**: Because this is built on `@base-ui/react`, it maintains standard WAI-ARIA compliance for tab interfaces, including keyboard navigation (arrow keys) and focus management.
- **Customization**: You can override styles by passing a `className` to any of the exported components, which will be merged via the `cn` utility.