import { useQuery } from "@tanstack/react-query"
import { apiFetch } from "./client"

export interface AgentInfoResponse {
  agentId: string
  hostname: string
  totalCpuCores: number
  availableCpuCores: number
  totalMemoryMB: number
  availableMemoryMB: number
  activeWorkerIds: string[]
  supportedExecutors: string[]
  lastHeartbeat: string | null
}

export function useAgents() {
  return useQuery({
    queryKey: ["agents"],
    queryFn: () => apiFetch<AgentInfoResponse[]>("/api/admin/agents"),
    staleTime: 15_000,
  })
}
