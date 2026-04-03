import { useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { ArrowLeft, Trash2, Play } from "lucide-react"
import { formatDistanceToNow } from "date-fns"
import type { ExecutorType } from "@/api/types"

interface SavedTemplate {
  name: string
  executorType: ExecutorType
  json: string
  createdAt: string
}

function loadTemplates(): SavedTemplate[] {
  try {
    return JSON.parse(localStorage.getItem("cb-templates") || "[]")
  } catch {
    return []
  }
}

function saveTemplates(templates: SavedTemplate[]) {
  localStorage.setItem("cb-templates", JSON.stringify(templates))
}

export default function TaskTemplates() {
  const navigate = useNavigate()
  const [templates, setTemplates] = useState(loadTemplates)

  const handleDelete = (index: number) => {
    const next = templates.filter((_, i) => i !== index)
    saveTemplates(next)
    setTemplates(next)
  }

  const handleLoad = (template: SavedTemplate) => {
    navigate("/tasks/submit", { state: { template } })
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <Link to="/tasks" className="inline-flex items-center gap-1 text-sm text-slate-400 hover:text-slate-200">
        <ArrowLeft className="h-4 w-4" /> Back to tasks
      </Link>

      <h1 className="text-xl font-semibold">Task Templates</h1>

      {templates.length === 0 ? (
        <div className="text-center py-12 text-sm text-slate-500">
          No saved templates. Create one from the Submit Task page.
        </div>
      ) : (
        <div className="rounded-md border border-slate-700 overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-slate-700 hover:bg-transparent">
                <TableHead className="text-xs">Name</TableHead>
                <TableHead className="text-xs">Executor</TableHead>
                <TableHead className="text-xs">Created</TableHead>
                <TableHead className="text-xs w-24">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {templates.map((t, i) => (
                <TableRow key={`${t.name}-${i}`} className="border-slate-700/50">
                  <TableCell className="text-sm">{t.name}</TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-xs">{t.executorType}</Badge>
                  </TableCell>
                  <TableCell className="text-xs text-slate-400">
                    {formatDistanceToNow(new Date(t.createdAt), { addSuffix: true })}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="sm" onClick={() => handleLoad(t)}>
                        <Play className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => handleDelete(i)} className="text-red-400 hover:text-red-300">
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
