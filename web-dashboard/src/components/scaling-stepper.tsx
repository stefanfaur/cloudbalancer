import { useMemo } from "react"
import { X } from "lucide-react"
import type { ScalingOperation } from "@/hooks/use-scaling-progress"

// Format timestamp for display
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString("en-US", { hour12: false, hour: "2-digit", minute: "2-digit", second: "2-digit" })
}

// Stepper step definitions
const SCALE_UP_STEPS: Array<{ id: string; label: string }> = [
  { id: "decision", label: "Decision" },
  { id: "command_sent", label: "Cmd Sent" },
  { id: "container_starting", label: "Starting" },
  { id: "registered", label: "Registered" },
  { id: "ready", label: "Ready" },
]

const SCALE_DOWN_STEPS: Array<{ id: string; label: string }> = [
  { id: "decision", label: "Decision" },
  { id: "command_sent", label: "Cmd Sent" },
  { id: "draining", label: "Draining" },
  { id: "stopped", label: "Stopped" },
]

interface StepCircleProps {
  step: string
  label: string
  timestamp?: number
  isCompleted: boolean
  isInProgress: boolean
  isFailed: boolean
}

function StepCircle({ step, label, timestamp, isCompleted, isInProgress, isFailed }: StepCircleProps) {
  let circleClasses = "h-5 w-5 rounded-full flex items-center justify-center transition-all duration-150"

  if (isFailed) {
    circleClasses += " bg-red-900/30 border border-red-500"
  } else if (isCompleted) {
    circleClasses += " bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.4)]"
  } else if (isInProgress) {
    circleClasses += " bg-blue-400 animate-pulse border border-blue-300"
  } else {
    circleClasses += " border border-slate-600 bg-slate-900"
  }

  return (
    <div className="flex flex-col items-center">
      <div className={circleClasses}>
        {isFailed ? <X className="h-3 w-3 text-red-400" /> : null}
      </div>
      <div className="text-[9px] text-slate-400 mt-1 text-center w-12">{label}</div>
      {timestamp && isCompleted && (
        <div className="text-[8px] text-slate-500 font-mono mt-0.5">{formatTime(timestamp)}</div>
      )}
    </div>
  )
}

interface StepLineProps {
  isCompleted: boolean
}

function StepLine({ isCompleted }: StepLineProps) {
  return (
    <div className={`h-1 flex-1 mx-1 ${isCompleted ? "bg-emerald-400" : "bg-slate-700"}`} />
  )
}

interface ScalingStepperProps {
  operations: ScalingOperation[]
}

export function ScalingStepper({ operations }: ScalingStepperProps) {
  if (operations.length === 0) return null

  return (
    <div className="space-y-3 mt-3 pl-4 pr-2 py-3 bg-slate-950/50 rounded border border-slate-700/30">
      {operations.map((op) => {
        const steps = op.direction === "up" ? SCALE_UP_STEPS : SCALE_DOWN_STEPS
        const currentStepIndex = steps.findIndex((s) => s.id === op.currentStep)

        return (
          <div key={`${op.workerId}-${op.direction}`} className="space-y-2">
            {/* Worker label with direction indicator */}
            <div className="flex items-center gap-2">
              <span className={`text-lg ${op.direction === "up" ? "text-emerald-400" : "text-amber-400"}`}>
                {op.direction === "up" ? "↑" : "↓"}
              </span>
              <span className="text-xs font-mono text-slate-300">{op.workerId}</span>
              {op.failed && <span className="text-[9px] text-red-400 ml-auto">{op.failed.error}</span>}
            </div>

            {/* Step progression */}
            <div className="flex items-end gap-0">
              {steps.map((step, idx) => {
                const isCompleted = idx < currentStepIndex
                const isInProgress = idx === currentStepIndex && !op.failed && !op.completedAt
                const isFailed = op.failed ? true : false
                const timestamp = op.stepTimestamps[step.id as keyof typeof op.stepTimestamps]

                return (
                  <div key={step.id} className="flex items-end">
                    <StepCircle
                      step={step.id}
                      label={step.label}
                      timestamp={timestamp}
                      isCompleted={isCompleted}
                      isInProgress={isInProgress}
                      isFailed={isFailed}
                    />
                    {idx < steps.length - 1 && <StepLine isCompleted={isCompleted} />}
                  </div>
                )
              })}
            </div>

            {/* Completion info */}
            {op.completedAt && (
              <div className="text-[9px] text-slate-500 pl-1">
                Completed in {Math.round((op.completedAt - op.startedAt) / 1000)}s
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
