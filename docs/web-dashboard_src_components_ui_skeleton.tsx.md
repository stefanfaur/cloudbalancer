# File: web-dashboard/src/components/ui/skeleton.tsx

## Overview

The `Skeleton` component is a reusable UI element used to provide a visual placeholder while content is loading. It implements the standard "pulse" animation pattern to indicate that data is being fetched or processed, improving the perceived performance and user experience of the dashboard.

## Public API

### `Skeleton`

A functional component that renders a `div` element styled as a loading placeholder.

**Props:**
* `className` (optional): A string of additional CSS classes to override or extend the default styling.
* `...props`: Any standard HTML `div` attributes (e.g., `id`, `style`, `aria-label`).

**Implementation Details:**
* Uses the `cn` utility function to merge default classes (`animate-pulse`, `rounded-md`, `bg-muted`) with user-provided `className`.
* Includes a `data-slot="skeleton"` attribute for easier identification in DOM inspection and testing.

## Dependencies

* `web-dashboard/src/lib/utils.ts`: Provides the `cn` utility for conditional class name merging.
* `react`: Utilizes `React.ComponentProps` for type safety.

## Usage Notes

* **Styling**: The component defaults to `bg-muted`. You can override the background color or border radius by passing a custom `className`.
* **Layout**: Since the component renders a `div`, it behaves like a block-level element. Ensure you wrap it in a container or apply flex/grid properties to the `className` if you need to control its dimensions (e.g., `w-full h-4`).
* **Common Use Cases**:
    * Replacing text lines while data fetches.
    * Creating placeholder cards for lists or grids.
    * Providing visual feedback in `task-detail` or `worker-list` views before the API response is received.

**Example:**
```tsx
import { Skeleton } from "@/components/ui/skeleton";

function LoadingState() {
  return (
    <div className="space-y-2">
      <Skeleton className="h-4 w-[250px]" />
      <Skeleton className="h-4 w-[200px]" />
    </div>
  );
}
```