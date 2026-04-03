import { useState, useMemo } from "react"
import { Link, useNavigate } from "react-router-dom"
import { useSubmitTask } from "@/api/tasks"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { ArrowLeft, Check, AlertTriangle, Save } from "lucide-react"
import { toast } from "sonner"
import type { ExecutorType } from "@/api/types"

const TEMPLATES: Record<ExecutorType, object> = {
  SIMULATED: { executorType: "SIMULATED", executionSpec: { durationMs: 5000, failureProbability: 0.0 }, priority: "NORMAL" },
  SHELL: { executorType: "SHELL", executionSpec: { command: "echo hello" }, priority: "NORMAL" },
  DOCKER: { executorType: "DOCKER", executionSpec: { image: "alpine:latest", command: ["echo", "hello"] }, priority: "NORMAL", resourceProfile: { cpuCores: 1, memoryMB: 256, diskMB: 100, gpuRequired: false, estimatedDurationSeconds: 60, networkAccessRequired: false } },
  PYTHON: { executorType: "PYTHON", executionSpec: { script: "print('hello')", requirements: [] }, priority: "NORMAL" },
}

const EXECUTOR_TYPES: ExecutorType[] = ["SIMULATED", "SHELL", "DOCKER", "PYTHON"]

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

export default function TaskSubmit() {
  const navigate = useNavigate()
  const submitTask = useSubmitTask()
  const [executorType, setExecutorType] = useState<ExecutorType>("SIMULATED")
  const [json, setJson] = useState(JSON.stringify(TEMPLATES.SIMULATED, null, 2))
  const [validation, setValidation] = useState<{ valid: boolean; error?: string } | null>(null)
  const [templateName, setTemplateName] = useState("")
  const [showSaveDialog, setShowSaveDialog] = useState(false)
  const savedTemplates = useMemo(loadTemplates, [])

  const handleTypeChange = (type: ExecutorType) => {
    setExecutorType(type)
    setJson(JSON.stringify(TEMPLATES[type], null, 2))
    setValidation(null)
  }

  const handleValidate = () => {
    try {
      JSON.parse(json)
      setValidation({ valid: true })
    } catch (e) {
      setValidation({ valid: false, error: (e as Error).message })
    }
  }

  const handleSubmit = async () => {
    try {
      const descriptor = JSON.parse(json)
      const task = await submitTask.mutateAsync(descriptor)
      toast.success("Task submitted", {
        description: task.id,
        action: {
          label: "View",
          onClick: () => navigate(`/tasks/${task.id}`),
        },
      })
      navigate("/tasks")
    } catch (e) {
      toast.error("Submit failed", { description: (e as Error).message })
    }
  }

  const handleSaveTemplate = () => {
    if (!templateName.trim()) return
    const templates = loadTemplates()
    templates.push({
      name: templateName.trim(),
      executorType,
      json,
      createdAt: new Date().toISOString(),
    })
    saveTemplates(templates)
    setShowSaveDialog(false)
    setTemplateName("")
    toast.success("Template saved")
  }

  const handleLoadTemplate = (template: SavedTemplate) => {
    setExecutorType(template.executorType)
    setJson(template.json)
    setValidation(null)
  }

  // Line numbers
  const lineCount = json.split("\n").length

  return (
    <div className="space-y-6 max-w-3xl">
      <Link to="/tasks" className="inline-flex items-center gap-1 text-sm text-slate-400 hover:text-slate-200">
        <ArrowLeft className="h-4 w-4" /> Back to tasks
      </Link>

      <h1 className="text-xl font-semibold">Submit Task</h1>

      {/* Executor type selector */}
      <div className="flex items-center gap-2">
        <span className="text-sm text-slate-400">Executor:</span>
        {EXECUTOR_TYPES.map((type) => (
          <Button
            key={type}
            size="sm"
            variant={executorType === type ? "default" : "outline"}
            onClick={() => handleTypeChange(type)}
          >
            {type}
          </Button>
        ))}
      </div>

      {/* Saved templates */}
      {savedTemplates.length > 0 && (
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-slate-500">Templates:</span>
          {savedTemplates.map((t) => (
            <button
              key={t.name}
              onClick={() => handleLoadTemplate(t)}
              className="text-xs text-blue-400 hover:underline"
            >
              {t.name}
            </button>
          ))}
          <Link to="/tasks/templates" className="text-xs text-slate-500 hover:text-slate-300">
            Manage →
          </Link>
        </div>
      )}

      {/* JSON editor */}
      <Card className="bg-slate-900 border-slate-700">
        <CardContent className="p-0">
          <div className="flex">
            {/* Line numbers */}
            <div className="py-3 px-2 text-right select-none border-r border-slate-700">
              {Array.from({ length: lineCount }).map((_, i) => (
                <div key={i} className="text-[10px] font-mono text-slate-600 leading-5">
                  {i + 1}
                </div>
              ))}
            </div>
            {/* Editor */}
            <textarea
              value={json}
              onChange={(e) => { setJson(e.target.value); setValidation(null) }}
              className="flex-1 bg-transparent p-3 font-mono text-xs text-slate-200 leading-5 resize-none outline-none min-h-[300px]"
              spellCheck={false}
            />
          </div>
        </CardContent>
      </Card>

      {/* Validation feedback */}
      {validation && (
        <div className={`flex items-center gap-2 text-sm ${validation.valid ? "text-emerald-400" : "text-red-400"}`}>
          {validation.valid ? <Check className="h-4 w-4" /> : <AlertTriangle className="h-4 w-4" />}
          {validation.valid ? "Valid JSON" : validation.error}
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2">
        <Button variant="outline" onClick={handleValidate}>Validate</Button>
        <Button onClick={handleSubmit} disabled={submitTask.isPending}>
          {submitTask.isPending ? "Submitting..." : "Submit"}
        </Button>
        <Button variant="ghost" onClick={() => setShowSaveDialog(!showSaveDialog)}>
          <Save className="h-4 w-4 mr-1" />
          Save as Template
        </Button>
      </div>

      {/* Save template inline */}
      {showSaveDialog && (
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
            placeholder="Template name..."
            className="h-8 w-48 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200 placeholder:text-slate-500"
            onKeyDown={(e) => { if (e.key === "Enter") handleSaveTemplate() }}
          />
          <Button size="sm" onClick={handleSaveTemplate} disabled={!templateName.trim()}>Save</Button>
        </div>
      )}
    </div>
  )
}
