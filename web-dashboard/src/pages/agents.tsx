import { useAgents, type AgentInfoResponse } from "@/api/agents"
import { useTriggerScaling } from "@/api/scaling"
import { ErrorCard } from "@/components/error-card"
import { ScalingStepper } from "@/components/scaling-stepper"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Cpu, ChevronUp, ChevronDown } from "lucide-react"
import { toast } from "sonner"
import { useScalingProgress } from "@/hooks/use-scaling-progress"

function timeAgo(iso: string | null) {
  if (!iso) return "—"
  const diff = Date.now() - new Date(iso).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return `${seconds}s ago`
  return `${Math.floor(seconds / 60)}m ago`
}

function CapacityBar({ used, total, label, unit }: { used: number; total: number; label: string; unit: string }) {
  const pct = total > 0 ? Math.min((used / total) * 100, 100) : 0
  const color = pct > 85 ? "bg-red-500" : pct > 60 ? "bg-amber-500" : "bg-emerald-500"
  const fmt = (v: number) => (v >= 1024 && unit === "MB") ? `${(v / 1024).toFixed(1)} GB` : `${Math.round(v)} ${unit}`
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-[10px] text-slate-400">
        <span>{label}</span>
        <span>{fmt(used)} / {fmt(total)}</span>
      </div>
      <div className="h-1.5 w-28 bg-slate-700 rounded-full overflow-hidden">
        <div className={`h-full ${color} rounded-full`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

interface AgentRowProps {
  agent: AgentInfoResponse
}

function AgentRow({ agent }: AgentRowProps) {
  const trigger = useTriggerScaling()

  const cpuUsed = agent.totalCpuCores - agent.availableCpuCores
  const memUsed = agent.totalMemoryMB - agent.availableMemoryMB

  function handleScale(action: "SCALE_UP" | "SCALE_DOWN") {
    trigger.mutate(
      { action, count: 1, agentId: agent.agentId },
      {
        onSuccess: () => toast.success("Scale command sent"),
        onError: (e) => toast.error("Scale failed", { description: e.message }),
      },
    )
  }

  return (
    <TableRow className="border-slate-700/50">
      <TableCell className="font-mono text-xs text-slate-300">{agent.agentId}</TableCell>
      <TableCell className="text-xs text-slate-400">{agent.hostname}</TableCell>
      <TableCell>
        <CapacityBar used={cpuUsed} total={agent.totalCpuCores} label="CPU" unit="cores" />
      </TableCell>
      <TableCell>
        <CapacityBar used={memUsed} total={agent.totalMemoryMB} label="Memory" unit="MB" />
      </TableCell>
      <TableCell className="text-xs font-mono text-slate-300">
        {agent.activeWorkerIds.length}
      </TableCell>
      <TableCell className="text-xs text-slate-400">
        {agent.supportedExecutors.join(", ") || "—"}
      </TableCell>
      <TableCell className="text-xs text-slate-400 font-mono">
        {timeAgo(agent.lastHeartbeat)}
      </TableCell>
      <TableCell>
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950"
            onClick={() => handleScale("SCALE_UP")}
          >
            <ChevronUp className="h-3 w-3 mr-1" />
            Scale Up
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs text-amber-400 hover:text-amber-300 hover:bg-amber-950"
            onClick={() => handleScale("SCALE_DOWN")}
          >
            <ChevronDown className="h-3 w-3 mr-1" />
            Scale Down
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}

export default function AgentList() {
  const { data: agents, isLoading, isError, error, refetch } = useAgents()
  const progress = useScalingProgress()

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Agents</h1>

      {isError ? (
        <ErrorCard error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
        </div>
      ) : agents?.length === 0 ? (
        <div className="text-center py-12 text-sm text-slate-500">
          <Cpu className="h-8 w-8 mx-auto mb-2 text-slate-600" />
          No agents connected. Start an agent to begin.
        </div>
      ) : (
        <div className="rounded-md border border-slate-700 overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-slate-700 hover:bg-transparent">
                <TableHead className="text-xs">Agent ID</TableHead>
                <TableHead className="text-xs">Hostname</TableHead>
                <TableHead className="text-xs">CPU</TableHead>
                <TableHead className="text-xs">Memory</TableHead>
                <TableHead className="text-xs">Workers</TableHead>
                <TableHead className="text-xs">Executors</TableHead>
                <TableHead className="text-xs">Last Heartbeat</TableHead>
                <TableHead className="text-xs">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {agents?.map((a) => (
                <>
                  <AgentRow key={a.agentId} agent={a} />
                  <TableRow className="border-slate-700/50 bg-slate-950/20">
                    <TableCell colSpan={8}>
                      <ScalingStepper operations={progress.getOperationsForAgent(a.agentId)} />
                    </TableCell>
                  </TableRow>
                </>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
