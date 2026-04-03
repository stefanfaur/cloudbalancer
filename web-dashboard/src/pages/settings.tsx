import { useState, useEffect } from "react"
import { useAuth } from "@/hooks/use-auth"
import { useStrategy, useUpdateStrategy, useKillWorker, useFailTask, useInjectLatency } from "@/api/admin"
import { useScalingStatus, useUpdateScalingPolicy, useTriggerScaling } from "@/api/scaling"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { toast } from "sonner"
import { ShieldAlert } from "lucide-react"

const STRATEGIES = ["ROUND_ROBIN", "LEAST_CONNECTIONS", "RESOURCE_FIT", "CUSTOM"]

function StrategySection() {
  const strategy = useStrategy()
  const updateStrategy = useUpdateStrategy()
  const [selected, setSelected] = useState("")
  const [weights, setWeights] = useState<Record<string, number>>({})

  useEffect(() => {
    if (strategy.data) {
      setSelected(strategy.data.strategy)
      setWeights(strategy.data.weights ?? {})
    }
  }, [strategy.data])

  const handleSave = async () => {
    try {
      await updateStrategy.mutateAsync({
        strategy: selected,
        weights: selected === "CUSTOM" ? weights : undefined,
      })
      toast.success("Strategy updated")
    } catch (e) {
      toast.error("Failed to update strategy", { description: (e as Error).message })
    }
  }

  if (strategy.isLoading) return <Skeleton className="h-24 w-full" />

  return (
    <Card className="bg-slate-900 border-slate-700">
      <CardContent className="p-4 space-y-3">
        <div className="flex items-center gap-2">
          <Label className="text-xs text-slate-400">Strategy</Label>
          <select
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
            className="h-8 rounded-md border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200"
          >
            {STRATEGIES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
        {selected === "CUSTOM" && (
          <div className="space-y-2">
            <Label className="text-xs text-slate-400">Weights</Label>
            {["cpu", "memory", "taskCount", "latency"].map((key) => (
              <div key={key} className="flex items-center gap-2">
                <span className="text-xs text-slate-400 w-20">{key}</span>
                <Input
                  type="number"
                  step="0.1"
                  value={weights[key] ?? 1}
                  onChange={(e) => setWeights((prev) => ({ ...prev, [key]: parseFloat(e.target.value) || 0 }))}
                  className="w-20 h-7 text-xs bg-slate-800 border-slate-700"
                />
              </div>
            ))}
          </div>
        )}
        <Button size="sm" onClick={handleSave} disabled={updateStrategy.isPending}>
          {updateStrategy.isPending ? "Saving..." : "Save Strategy"}
        </Button>
      </CardContent>
    </Card>
  )
}

function ScalingPolicySection() {
  const scaling = useScalingStatus()
  const updatePolicy = useUpdateScalingPolicy()
  const triggerScaling = useTriggerScaling()
  const [form, setForm] = useState({
    minWorkers: 1,
    maxWorkers: 10,
    cooldownSeconds: 60,
    scaleUpStep: 1,
    scaleDownStep: 1,
    drainTimeSeconds: 30,
  })
  const [scaleCount, setScaleCount] = useState(1)
  const [confirmTrigger, setConfirmTrigger] = useState<"SCALE_UP" | "SCALE_DOWN" | null>(null)
  const [validationError, setValidationError] = useState("")

  useEffect(() => {
    if (scaling.data?.policy) {
      setForm(scaling.data.policy)
    }
  }, [scaling.data])

  const updateField = (key: keyof typeof form, value: string) => {
    const num = parseInt(value, 10) || 0
    setForm((prev) => ({ ...prev, [key]: num }))
    setValidationError("")
  }

  const handleSave = async () => {
    if (form.minWorkers >= form.maxWorkers) {
      setValidationError("Min workers must be less than max workers")
      return
    }
    if (Object.values(form).some((v) => v < 0)) {
      setValidationError("All values must be positive")
      return
    }
    try {
      await updatePolicy.mutateAsync(form)
      toast.success("Scaling policy updated")
    } catch (e) {
      toast.error("Failed to update policy", { description: (e as Error).message })
    }
  }

  const handleTrigger = async () => {
    if (!confirmTrigger) return
    try {
      await triggerScaling.mutateAsync({ action: confirmTrigger, count: scaleCount })
      toast.success(`Triggered ${confirmTrigger.replace("_", " ").toLowerCase()}`)
    } catch (e) {
      toast.error("Trigger failed", { description: (e as Error).message })
    }
    setConfirmTrigger(null)
  }

  if (scaling.isLoading) return <Skeleton className="h-48 w-full" />

  return (
    <Card className="bg-slate-900 border-slate-700">
      <CardContent className="p-4 space-y-4">
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {(Object.keys(form) as Array<keyof typeof form>).map((key) => (
            <div key={key}>
              <Label className="text-xs text-slate-400">{key.replace(/([A-Z])/g, " $1").trim()}</Label>
              <Input
                type="number"
                value={form[key]}
                onChange={(e) => updateField(key, e.target.value)}
                className="h-8 text-xs bg-slate-800 border-slate-700 mt-1"
              />
            </div>
          ))}
        </div>
        {validationError && <p className="text-xs text-red-400">{validationError}</p>}
        <Button size="sm" onClick={handleSave} disabled={updatePolicy.isPending}>
          {updatePolicy.isPending ? "Saving..." : "Save Policy"}
        </Button>

        {/* Manual triggers */}
        <div className="border-t border-slate-700 pt-3">
          <Label className="text-xs text-slate-400 mb-2 block">Manual Scaling</Label>
          <div className="flex items-center gap-2">
            <Input
              type="number"
              min="1"
              value={scaleCount}
              onChange={(e) => setScaleCount(parseInt(e.target.value, 10) || 1)}
              className="w-16 h-8 text-xs bg-slate-800 border-slate-700"
            />
            <Button size="sm" variant="secondary" onClick={() => setConfirmTrigger("SCALE_UP")}>Scale Up</Button>
            <Button size="sm" variant="outline" onClick={() => setConfirmTrigger("SCALE_DOWN")}>Scale Down</Button>
          </div>
        </div>

        <Dialog open={!!confirmTrigger} onOpenChange={(open) => { if (!open) setConfirmTrigger(null) }}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Confirm {confirmTrigger?.replace("_", " ")}</DialogTitle>
              <DialogDescription>
                {confirmTrigger === "SCALE_UP" ? `Add ${scaleCount} worker(s)?` : `Remove ${scaleCount} worker(s)?`}
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setConfirmTrigger(null)}>Cancel</Button>
              <Button onClick={handleTrigger}>Confirm</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  )
}

function ChaosMonkeySection() {
  const killWorker = useKillWorker()
  const failTask = useFailTask()
  const injectLatency = useInjectLatency()
  const [workerId, setWorkerId] = useState("")
  const [taskId, setTaskId] = useState("")
  const [latencyTarget, setLatencyTarget] = useState("")
  const [latencyDelay, setLatencyDelay] = useState(500)
  const [latencyDuration, setLatencyDuration] = useState(30)
  const [confirmAction, setConfirmAction] = useState<"kill" | "fail" | "latency" | null>(null)

  const handleConfirm = async () => {
    try {
      if (confirmAction === "kill") {
        await killWorker.mutateAsync(workerId || undefined)
        toast.success("Worker kill triggered")
      } else if (confirmAction === "fail") {
        await failTask.mutateAsync(taskId || undefined)
        toast.success("Task failure triggered")
      } else if (confirmAction === "latency") {
        await injectLatency.mutateAsync({ target: latencyTarget, delayMs: latencyDelay, durationSeconds: latencyDuration })
        toast.success("Latency injection triggered")
      }
    } catch (e) {
      toast.error("Chaos action failed", { description: (e as Error).message })
    }
    setConfirmAction(null)
  }

  return (
    <Card className="bg-slate-900 border-red-900/30">
      <CardContent className="p-4 space-y-4">
        <div className="flex items-center gap-2 text-red-400">
          <ShieldAlert className="h-4 w-4" />
          <span className="text-xs font-medium">Chaos Engineering — Use with caution</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {/* Kill Worker */}
          <Card className="bg-slate-800 border-slate-700">
            <CardContent className="p-3 space-y-2">
              <p className="text-xs font-medium">Kill Worker</p>
              <Input
                value={workerId}
                onChange={(e) => setWorkerId(e.target.value)}
                placeholder="Worker ID (optional)"
                className="h-7 text-xs bg-slate-900 border-slate-700"
              />
              <Button size="sm" variant="destructive" onClick={() => setConfirmAction("kill")} className="w-full">
                Kill
              </Button>
            </CardContent>
          </Card>

          {/* Fail Task */}
          <Card className="bg-slate-800 border-slate-700">
            <CardContent className="p-3 space-y-2">
              <p className="text-xs font-medium">Fail Task</p>
              <Input
                value={taskId}
                onChange={(e) => setTaskId(e.target.value)}
                placeholder="Task ID (optional)"
                className="h-7 text-xs bg-slate-900 border-slate-700"
              />
              <Button size="sm" variant="destructive" onClick={() => setConfirmAction("fail")} className="w-full">
                Fail
              </Button>
            </CardContent>
          </Card>

          {/* Inject Latency */}
          <Card className="bg-slate-800 border-slate-700">
            <CardContent className="p-3 space-y-2">
              <p className="text-xs font-medium">Inject Latency</p>
              <Input
                value={latencyTarget}
                onChange={(e) => setLatencyTarget(e.target.value)}
                placeholder="Target component"
                className="h-7 text-xs bg-slate-900 border-slate-700"
              />
              <div className="flex gap-1">
                <Input
                  type="number"
                  value={latencyDelay}
                  onChange={(e) => setLatencyDelay(parseInt(e.target.value, 10) || 0)}
                  placeholder="ms"
                  className="h-7 text-xs bg-slate-900 border-slate-700"
                />
                <Input
                  type="number"
                  value={latencyDuration}
                  onChange={(e) => setLatencyDuration(parseInt(e.target.value, 10) || 0)}
                  placeholder="sec"
                  className="h-7 text-xs bg-slate-900 border-slate-700"
                />
              </div>
              <Button size="sm" variant="destructive" onClick={() => setConfirmAction("latency")} className="w-full">
                Inject
              </Button>
            </CardContent>
          </Card>
        </div>

        <Dialog open={!!confirmAction} onOpenChange={(open) => { if (!open) setConfirmAction(null) }}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Confirm Chaos Action</DialogTitle>
              <DialogDescription>
                {confirmAction === "kill" && "Kill a worker? This will terminate the selected worker process."}
                {confirmAction === "fail" && "Force-fail a task? This will mark the task as failed."}
                {confirmAction === "latency" && `Inject ${latencyDelay}ms latency to ${latencyTarget || "random"} for ${latencyDuration}s?`}
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
              <Button variant="destructive" onClick={handleConfirm}>Confirm</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  )
}

export default function SettingsPage() {
  const { role } = useAuth()

  if (role !== "ADMIN") {
    return (
      <div className="text-center py-12">
        <ShieldAlert className="h-8 w-8 mx-auto mb-2 text-slate-600" />
        <p className="text-slate-400">Access Denied</p>
        <p className="text-xs text-slate-500 mt-1">Settings are only available to administrators.</p>
      </div>
    )
  }

  return (
    <div className="space-y-8 max-w-3xl">
      <h1 className="text-xl font-semibold">Settings</h1>

      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Scheduling Strategy</h2>
        <StrategySection />
      </section>

      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Auto-Scaling Policy</h2>
        <ScalingPolicySection />
      </section>

      <section>
        <h2 className="text-sm font-medium text-slate-400 mb-3">Chaos Monkey</h2>
        <ChaosMonkeySection />
      </section>
    </div>
  )
}
