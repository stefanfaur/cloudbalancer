# Symbol: web-dashboard.src.api.types.Role

## Purpose

The `Role` type alias defines the set of authorized access levels within the `web-dashboard` application. It acts as a strict union type to ensure type safety when handling user permissions, API access control, and conditional UI rendering across the dashboard. By restricting values to a predefined set of strings, it prevents invalid authorization states throughout the application.

## Signature

```typescript
type Role = "ADMIN" | "OPERATOR" | "VIEWER" | "API_CLIENT"
```

## Parameters

*This symbol is a type alias and does not accept parameters.*

## Returns

*This symbol is a type alias and does not return a value.*

## Example Usage

The `Role` type is primarily used to type user objects, API response payloads, and authorization checks within components.

### Defining a User Object
```typescript
import { Role } from "../api/types";

interface User {
  id: string;
  username: string;
  role: Role;
}

const currentUser: User = {
  id: "u123",
  username: "admin_user",
  role: "ADMIN" // Valid
};
```

### Conditional Rendering in Components
```typescript
import { Role } from "../api/types";

interface Props {
  userRole: Role;
}

export const AdminPanel = ({ userRole }: Props) => {
  if (userRole !== "ADMIN") {
    return <div>Access Denied</div>;
  }

  return <div>Welcome to the Admin Dashboard</div>;
};
```

### Type Guarding
```typescript
function canManageTasks(role: Role): boolean {
  return ["ADMIN", "OPERATOR"].includes(role);
}
```