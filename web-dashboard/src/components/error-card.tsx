import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { AlertTriangle } from "lucide-react"

interface ErrorCardProps {
  error: Error
  onRetry: () => void
}

export function ErrorCard({ error, onRetry }: ErrorCardProps) {
  return (
    <Card className="bg-slate-900 border-red-900/30">
      <CardContent className="flex flex-col items-center gap-3 py-8">
        <AlertTriangle className="h-8 w-8 text-red-400" />
        <p className="text-sm text-red-300">{error.message || "Something went wrong."}</p>
        <Button variant="outline" size="sm" onClick={onRetry}>
          Retry
        </Button>
      </CardContent>
    </Card>
  )
}
