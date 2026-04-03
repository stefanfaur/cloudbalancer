import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { apiFetch } from "./client"
import type { TaskPageResponse, TaskEnvelope, TaskLogsResponse, BulkResultEntry, Priority } from "./types"

interface TaskFilters {
  offset?: number
  limit?: number
  status?: string
  priority?: string
  executorType?: string
  workerId?: string
  since?: string
}

function buildTaskParams(filters?: TaskFilters): string {
  const params = new URLSearchParams()
  if (filters?.offset != null) params.set("offset", String(filters.offset))
  if (filters?.limit != null) params.set("limit", String(filters.limit))
  if (filters?.status) params.set("status", filters.status)
  if (filters?.priority) params.set("priority", filters.priority)
  if (filters?.executorType) params.set("executorType", filters.executorType)
  if (filters?.workerId) params.set("workerId", filters.workerId)
  if (filters?.since) params.set("since", filters.since)
  const str = params.toString()
  return str ? `?${str}` : ""
}

export function useTasks(filters?: TaskFilters) {
  return useQuery({
    queryKey: ["tasks", filters],
    queryFn: () => apiFetch<TaskPageResponse>(`/api/tasks${buildTaskParams(filters)}`),
    staleTime: 15_000,
  })
}

export function useTask(id: string) {
  return useQuery({
    queryKey: ["task", id],
    queryFn: () => apiFetch<TaskEnvelope>(`/api/tasks/${id}`),
    staleTime: 10_000,
    enabled: !!id,
  })
}

export function useTaskLogs(id: string) {
  return useQuery({
    queryKey: ["task-logs", id],
    queryFn: () => apiFetch<TaskLogsResponse>(`/api/tasks/${id}/logs`),
    staleTime: 5_000,
    enabled: !!id,
  })
}

export function useSubmitTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (descriptor: Record<string, unknown>) =>
      apiFetch<TaskEnvelope>("/api/tasks", { method: "POST", body: JSON.stringify(descriptor) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tasks"] }),
  })
}

export function useBulkCancel() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (taskIds: string[]) =>
      apiFetch<BulkResultEntry[]>("/api/tasks/bulk/cancel", {
        method: "POST",
        body: JSON.stringify({ taskIds }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tasks"] }),
  })
}

export function useBulkRetry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (taskIds: string[]) =>
      apiFetch<BulkResultEntry[]>("/api/tasks/bulk/retry", {
        method: "POST",
        body: JSON.stringify({ taskIds }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tasks"] }),
  })
}

export function useBulkReprioritize() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: { taskIds: string[]; priority: Priority }) =>
      apiFetch<BulkResultEntry[]>("/api/tasks/bulk/reprioritize", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tasks"] }),
  })
}
