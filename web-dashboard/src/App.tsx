import { lazy, Suspense, type ReactNode } from "react"
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { AuthProvider, useAuth } from "@/hooks/use-auth"
import { AlertsProvider } from "@/hooks/use-alerts"
import { Skeleton } from "@/components/ui/skeleton"

// Eager: login and layout (always needed)
import LoginPage from "@/pages/login"
import DashboardLayout from "@/layouts/dashboard-layout"

// Lazy: route-based code splitting
const ClusterOverview = lazy(() => import("@/pages/cluster-overview"))
const TaskList = lazy(() => import("@/pages/tasks/task-list"))
const TaskDetail = lazy(() => import("@/pages/tasks/task-detail"))
const TaskSubmit = lazy(() => import("@/pages/tasks/task-submit"))
const TaskTemplates = lazy(() => import("@/pages/tasks/task-templates"))
const WorkerList = lazy(() => import("@/pages/workers/worker-list"))
const WorkerDetail = lazy(() => import("@/pages/workers/worker-detail"))
const Analytics = lazy(() => import("@/pages/analytics"))
const SettingsPage = lazy(() => import("@/pages/settings"))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function LoadingFallback() {
  return (
    <div className="space-y-4 p-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
      <Skeleton className="h-64 w-full" />
    </div>
  )
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()
  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <div className="text-slate-400">Loading...</div>
      </div>
    )
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Navigate to="/cluster" replace />
          </ProtectedRoute>
        }
      />
      <Route
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/cluster" element={<ClusterOverview />} />
        <Route path="/tasks" element={<TaskList />} />
        <Route path="/tasks/:id" element={<TaskDetail />} />
        <Route path="/tasks/submit" element={<TaskSubmit />} />
        <Route path="/tasks/templates" element={<TaskTemplates />} />
        <Route path="/workers" element={<WorkerList />} />
        <Route path="/workers/:id" element={<WorkerDetail />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <AlertsProvider>
            <Suspense fallback={<LoadingFallback />}>
              <AppRoutes />
            </Suspense>
          </AlertsProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
