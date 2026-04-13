# File: web-dashboard/src/lib/utils.ts

## Overview

The `web-dashboard/src/lib/utils.ts` file serves as a centralized utility module for the `web-dashboard` project. Its primary purpose is to provide a robust, standardized way to handle CSS class manipulation within a Tailwind CSS environment. By abstracting the logic for conditional class merging and conflict resolution, it ensures consistent styling across the application's UI components.

## Public API

### `cn`

```typescript
function cn(...inputs: ClassValue[]): string
```

The `cn` function is a utility that merges multiple Tailwind CSS class names. It accepts a variable number of arguments (`inputs`), which can be strings, objects, or arrays (as defined by `ClassValue` from `clsx`).

- **Parameters**:
  - `...inputs`: A spread of `ClassValue` types, allowing for conditional logic (e.g., `{ 'bg-red': hasError }`) and standard class strings.
- **Returns**: A single, merged string of CSS classes with Tailwind conflicts resolved.

## Dependencies

- **[clsx](https://github.com/lukeed/clsx)**: Used for conditional class name construction.
- **[tailwind-merge](https://github.com/dcastil/tailwind-merge)**: Used to intelligently merge Tailwind CSS classes, ensuring that later classes override conflicting earlier ones (e.g., `px-2 px-4` becomes `px-4`).

## Usage Notes

The `cn` function is the standard approach for applying styles in this project, particularly when building reusable UI components. It is widely utilized across the `components/ui` directory and various page layouts to handle dynamic styling based on component props.

### Example Usage

```typescript
import { cn } from "@/lib/utils";

// Basic usage
const className = cn("text-sm font-medium", "text-gray-500");

// Conditional usage
const buttonClass = cn(
  "px-4 py-2 rounded",
  isActive ? "bg-blue-500 text-white" : "bg-gray-200"
);
```

By using `cn`, you ensure that your components remain clean and that Tailwind class conflicts are handled automatically, preventing common issues where default styles are not correctly overridden by prop-based overrides.