import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { apiFetch } from "./client"
import type { ScalingStatusResponse, ScalingPolicy } from "./types"

export function useScalingStatus() {
  return useQuery({
    queryKey: ["scaling-status"],
    queryFn: () => apiFetch<ScalingStatusResponse>("/api/scaling/status"),
    staleTime: 30_000,
  })
}

export function useUpdateScalingPolicy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (policy: Partial<ScalingPolicy>) =>
      apiFetch<void>("/api/scaling/policy", { method: "PUT", body: JSON.stringify(policy) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["scaling-status"] }),
  })
}

export function useTriggerScaling() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: { action: string; count?: number; agentId?: string }) =>
      apiFetch<void>("/api/scaling/trigger", { method: "POST", body: JSON.stringify(data) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["scaling-status"] }),
  })
}
