import { useQuery } from "@tanstack/react-query"
import { apiFetch } from "./client"
import type { WorkerMetricsSnapshot, WorkerMetricsBucket, ClusterMetrics } from "./types"

export function useWorkerSnapshots() {
  return useQuery({
    queryKey: ["worker-snapshots"],
    queryFn: () => apiFetch<WorkerMetricsSnapshot[]>("/api/metrics/workers"),
    staleTime: 30_000,
  })
}

export function useWorkerHistory(
  id: string,
  from?: string,
  to?: string,
  bucket?: string,
) {
  return useQuery({
    queryKey: ["worker-history", id, from, to, bucket],
    queryFn: () => {
      const params = new URLSearchParams()
      if (from) params.set("from", from)
      if (to) params.set("to", to)
      if (bucket) params.set("bucket", bucket)
      const qs = params.toString()
      return apiFetch<WorkerMetricsBucket[]>(`/api/metrics/workers/${id}/history${qs ? `?${qs}` : ""}`)
    },
    staleTime: 60_000,
    enabled: !!id,
  })
}

export function useClusterMetrics() {
  return useQuery({
    queryKey: ["cluster-metrics"],
    queryFn: () => apiFetch<ClusterMetrics>("/api/metrics/cluster"),
    staleTime: 30_000,
  })
}
