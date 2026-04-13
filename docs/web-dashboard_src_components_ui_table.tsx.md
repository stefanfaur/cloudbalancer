# File: web-dashboard/src/components/ui/table.tsx

## Overview

The `web-dashboard/src/components/ui/table.tsx` file provides a suite of modular, accessible, and responsive table components built using React. It is designed to provide a consistent look and feel across the dashboard, handling common patterns like hover states, selection highlighting, and responsive horizontal scrolling.

**Note:** This file is a **HOTSPOT** in the codebase. It is frequently modified and serves as a foundational UI primitive for multiple pages (e.g., Task lists, Worker management). Changes to this file carry a high risk of regression across the entire application. Exercise caution when modifying styles or structural markup.

## Public API

The module exports the following components, which map directly to standard HTML table elements with pre-applied Tailwind CSS utility classes:

*   **`Table`**: The wrapper component. It provides a responsive container (`overflow-x-auto`) for the table.
*   **`TableHeader`**: Renders the `<thead>` element.
*   **`TableBody`**: Renders the `<tbody>` element.
*   **`TableFooter`**: Renders the `<tfoot>` element with muted background styling.
*   **`TableRow`**: Renders a `<tr>` element. Includes built-in support for hover effects, `aria-expanded` states, and selection states (`data-[state=selected]`).
*   **`TableHead`**: Renders a `<th>` element. Optimized for headers with left-aligned text and whitespace handling.
*   **`TableCell`**: Renders a `<td>` element. Optimized for data cells with consistent padding.
*   **`TableCaption`**: Renders a `<caption>` element for table accessibility.

## Dependencies

*   **`react`**: Used for component definitions and `ComponentProps` typing.
*   **`@/lib/utils`**: Imports the `cn` utility function, which is used to merge Tailwind classes efficiently and handle conditional class application.

## Usage Notes

### Implementation Rationale
The components are built as "headless" wrappers around standard HTML tags. By using `React.ComponentProps`, each component maintains full compatibility with standard HTML attributes (e.g., `onClick`, `id`, `style`), allowing for maximum flexibility while enforcing a consistent design system via Tailwind classes.

### Common Pitfalls
1.  **Responsive Overflow**: Always wrap your `<table>` inside the `Table` component. The `Table` component provides the `overflow-x-auto` container necessary to prevent layout breakage on smaller screens.
2.  **Checkbox Handling**: The `TableHead` and `TableCell` components include specific selectors `[&:has([role=checkbox])]:pr-0`. This automatically removes padding when a checkbox is present in the cell, ensuring proper alignment. Avoid overriding this unless necessary.
3.  **Selection States**: To trigger the "selected" background color on a `TableRow`, ensure the `data-state="selected"` attribute is applied to the row element.

### Example Usage

```tsx
import { 
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell 
} from "@/components/ui/table"

export function MyDataList({ data }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead>Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.map((item) => (
          <TableRow key={item.id}>
            <TableCell>{item.name}</TableCell>
            <TableCell>{item.status}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
```

### Extending Styles
Because the components use the `cn` utility, you can easily override or extend styles by passing a `className` prop:

```tsx
<TableRow className="bg-blue-50 hover:bg-blue-100">
  <TableCell>Custom Row</TableCell>
</TableRow>
```