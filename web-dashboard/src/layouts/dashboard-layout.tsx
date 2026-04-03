import { Outlet, NavLink, useNavigate } from "react-router-dom"
import { useAuth } from "@/hooks/use-auth"
import { useWebSocket } from "@/hooks/use-websocket"
import { ConnectionIndicator } from "@/components/connection-indicator"
import { AlertsBanner } from "@/components/alerts-banner"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import {
  LayoutDashboard,
  ListChecks,
  Server,
  BarChart3,
  Settings,
  LogOut,
} from "lucide-react"

const NAV_ITEMS = [
  { to: "/cluster", label: "Cluster", icon: LayoutDashboard },
  { to: "/tasks", label: "Tasks", icon: ListChecks },
  { to: "/workers", label: "Workers", icon: Server },
  { to: "/analytics", label: "Analytics", icon: BarChart3 },
] as const

export default function DashboardLayout() {
  const { user, role, logout } = useAuth()
  const { isConnected, isReconnecting } = useWebSocket()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate("/login")
  }

  return (
    <div className="flex h-screen bg-slate-950 text-slate-50">
      {/* Sidebar */}
      <aside className="flex flex-col border-r border-slate-700 bg-slate-900 w-56 xl:w-56 lg:w-14 shrink-0">
        {/* Logo */}
        <div className="flex items-center gap-2 px-4 h-14 border-b border-slate-700">
          <div className="h-7 w-7 rounded-md bg-blue-600 flex items-center justify-center text-xs font-bold shrink-0">
            CB
          </div>
          <span className="text-sm font-semibold tracking-tight lg:hidden xl:inline">
            CloudBalancer
          </span>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-3 space-y-0.5 px-2">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors",
                  isActive
                    ? "bg-slate-800 text-slate-50"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50",
                )
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              <span className="lg:hidden xl:inline">{label}</span>
            </NavLink>
          ))}
          {role === "ADMIN" && (
            <NavLink
              to="/settings"
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors",
                  isActive
                    ? "bg-slate-800 text-slate-50"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50",
                )
              }
            >
              <Settings className="h-4 w-4 shrink-0" />
              <span className="lg:hidden xl:inline">Settings</span>
            </NavLink>
          )}
        </nav>

        {/* Bottom user section */}
        <div className="border-t border-slate-700 px-3 py-3 space-y-2">
          <ConnectionIndicator isConnected={isConnected} isReconnecting={isReconnecting} />
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="flex items-center justify-end gap-3 h-14 px-6 border-b border-slate-700 bg-slate-900 shrink-0">
          <span className="text-sm text-slate-300">{user}</span>
          {role && (
            <Badge variant="secondary" className="text-xs">
              {role}
            </Badge>
          )}
          <Button variant="ghost" size="sm" onClick={handleLogout} className="text-slate-400 hover:text-slate-200">
            <LogOut className="h-4 w-4" />
          </Button>
        </header>

        {/* Content */}
        <main className="flex-1 overflow-auto p-6">
          <AlertsBanner />
          <Outlet />
        </main>
      </div>
    </div>
  )
}
