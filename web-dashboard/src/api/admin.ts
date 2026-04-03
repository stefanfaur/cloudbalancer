import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { apiFetch } from "./client"
import type { StrategyResponse } from "./types"

export function useStrategy() {
  return useQuery({
    queryKey: ["strategy"],
    queryFn: () => apiFetch<StrategyResponse>("/api/admin/strategy"),
    staleTime: 60_000,
  })
}

export function useUpdateStrategy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: { strategy: string; weights?: Record<string, number> }) =>
      apiFetch<StrategyResponse>("/api/admin/strategy", { method: "PUT", body: JSON.stringify(data) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["strategy"] }),
  })
}

export function useUpdateWorkerTags() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: { workerId: string; tags: string[] }) =>
      apiFetch<string[]>(`/api/admin/workers/${data.workerId}/tags`, {
        method: "PUT",
        body: JSON.stringify({ tags: data.tags }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["worker-snapshots"] }),
  })
}

export function useKillWorker() {
  return useMutation({
    mutationFn: (workerId?: string) =>
      apiFetch<void>("/api/admin/chaos/kill-worker", {
        method: "POST",
        body: JSON.stringify({ workerId }),
      }),
  })
}

export function useFailTask() {
  return useMutation({
    mutationFn: (taskId?: string) =>
      apiFetch<void>("/api/admin/chaos/fail-task", {
        method: "POST",
        body: JSON.stringify({ taskId }),
      }),
  })
}

export function useInjectLatency() {
  return useMutation({
    mutationFn: (data: { target: string; delayMs: number; durationSeconds: number }) =>
      apiFetch<void>("/api/admin/chaos/inject-latency", {
        method: "POST",
        body: JSON.stringify(data),
      }),
  })
}
