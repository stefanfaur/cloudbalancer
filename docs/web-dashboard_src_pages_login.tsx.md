# File: web-dashboard/src/pages/login.tsx

## Overview

The `web-dashboard/src/pages/login.tsx` file implements the authentication interface for the CloudBalancer dashboard. It provides a secure, styled login form that captures user credentials and interfaces with the application's authentication provider to manage session state.

The page features a responsive design using UI components from the project's internal library, including `Card`, `Input`, `Label`, and `Button`. It handles loading states, error reporting, and post-authentication redirection to the dashboard root.

## Public API

### `handleSubmit`
`function handleSubmit(e: FormEvent)`

An asynchronous event handler triggered upon form submission. 

- **Parameters**: 
    - `e`: The `FormEvent` triggered by the login form submission.
- **Behavior**:
    1. Prevents the default form submission behavior.
    2. Resets previous error states and sets the `loading` state to `true`.
    3. Invokes the `login` method from the `useAuth` hook with the current `username` and `password` state values.
    4. On success, navigates the user to the dashboard root (`/`).
    5. On failure, updates the `error` state with a user-friendly message.
    6. Ensures the `loading` state is reset to `false` regardless of the outcome.

## Dependencies

This component relies on the following internal and external modules:

- **React Hooks**: `useState` for local state management, `useNavigate` for routing.
- **Authentication**: `web-dashboard/src/hooks/use-auth.tsx` for session management.
- **UI Components**:
    - `web-dashboard/src/components/ui/button.tsx`
    - `web-dashboard/src/components/ui/input.tsx`
    - `web-dashboard/src/components/ui/label.tsx`
    - `web-dashboard/src/components/ui/card.tsx`

## Usage Notes

- **State Management**: The component maintains local state for `username`, `password`, `error`, and `loading`. These are controlled inputs linked directly to the form fields.
- **Error Handling**: If the `login` function throws an exception, the UI displays a red alert message below the password field.
- **Loading State**: The "Sign in" button is automatically disabled while `loading` is true to prevent multiple simultaneous authentication requests.
- **Styling**: The component uses Tailwind CSS classes to maintain a dark-themed aesthetic consistent with the CloudBalancer dashboard design system.
- **Integration**: This page is registered in the main application router (`web-dashboard/src/App.tsx`) and is covered by unit tests in `web-dashboard/src/pages/__tests__/login.test.tsx`.