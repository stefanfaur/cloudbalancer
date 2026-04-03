import { Link } from "react-router-dom"
import type { LucideIcon } from "lucide-react"

interface EmptyStateProps {
  icon: LucideIcon
  message: string
  actionLabel?: string
  actionHref?: string
}

export function EmptyState({ icon: Icon, message, actionLabel, actionHref }: EmptyStateProps) {
  return (
    <div className="text-center py-16">
      <Icon className="h-10 w-10 mx-auto mb-3 text-slate-600" />
      <p className="text-sm text-slate-400">{message}</p>
      {actionLabel && actionHref && (
        <Link to={actionHref} className="inline-block mt-3 text-sm text-blue-400 hover:underline">
          {actionLabel}
        </Link>
      )}
    </div>
  )
}
