# File: web-dashboard/src/components/ui/sonner.tsx

## Overview

The `web-dashboard/src/components/ui/sonner.tsx` file provides a customized wrapper component for the `sonner` toast notification library. It integrates seamlessly with the application's theme system (`next-themes`) and standardizes the visual appearance of toast notifications using project-specific design tokens and icons from `lucide-react`.

## Public API

### `Toaster`

A functional React component that renders the `sonner` toast container. It accepts all standard `ToasterProps` defined by the `sonner` library.

**Props:**
*   `...props`: Inherits all properties from `ToasterProps` (e.g., `position`, `expand`, `richColors`, `duration`).

**Features:**
*   **Theme Integration**: Automatically syncs with the current application theme (light/dark/system) using `next-themes`.
*   **Custom Icons**: Maps standard toast types (`success`, `info`, `warning`, `error`, `loading`) to specific `lucide-react` icons.
*   **CSS Variable Mapping**: Injects CSS variables to ensure toasts inherit the application's CSS variables for background, text, border, and border-radius.

## Dependencies

*   **`sonner`**: The core toast notification library.
*   **`next-themes`**: Used to detect and apply the current theme to the toast container.
*   **`lucide-react`**: Provides the iconography for toast states.

## Usage Notes

To use the `Toaster` in your application, place it at the root level of your component tree (typically in your layout file):

```tsx
import { Toaster } from "@/components/ui/sonner"

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Toaster />
      </body>
    </html>
  )
}
```

**Customization**:
*   The component is pre-configured with CSS variables (`--normal-bg`, `--normal-text`, etc.) that map to your global CSS variables. Ensure your global CSS defines `--popover`, `--popover-foreground`, `--border`, and `--radius` for consistent styling.
*   The `toast` class is assigned the `cn-toast` identifier, which can be targeted in your global CSS files for additional layout or animation overrides.