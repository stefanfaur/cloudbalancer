// Enums
export type TaskState = "SUBMITTED" | "VALIDATED" | "QUEUED" | "ASSIGNED" | "PROVISIONING" | "RUNNING" | "POST_PROCESSING" | "COMPLETED" | "FAILED" | "TIMED_OUT" | "CANCELLED" | "DEAD_LETTERED"
export type Priority = "CRITICAL" | "HIGH" | "NORMAL" | "LOW"
export type ExecutorType = "SHELL" | "DOCKER" | "PYTHON" | "SIMULATED"
export type WorkerHealthState = "HEALTHY" | "SUSPECT" | "DEAD" | "RECOVERING" | "DRAINING"
export type Role = "ADMIN" | "OPERATOR" | "VIEWER" | "API_CLIENT"
export type ScalingAction = "SCALE_UP" | "SCALE_DOWN" | "NONE"

// Models
export interface ResourceProfile {
  cpuCores: number
  memoryMB: number
  diskMB: number
  gpuRequired: boolean
  estimatedDurationSeconds: number
  networkAccessRequired: boolean
}

export interface TaskConstraints {
  requiredTags: string[]
  blacklistedWorkers: string[]
  whitelistedWorkers: string[]
}

export interface ExecutionPolicy {
  maxRetries: number
  timeoutSeconds: number
  retryBackoffStrategy: string
  failureAction: string
}

export interface TaskIO {
  inputArtifacts?: InputArtifact[]
  outputArtifacts?: OutputArtifact[]
}

export interface InputArtifact {
  name: string
  location: string
  source: string
}

export interface OutputArtifact {
  name: string
  path: string
}

export interface TaskDescriptor {
  executorType: ExecutorType
  executionSpec: Record<string, unknown>
  resourceProfile?: ResourceProfile
  constraints?: TaskConstraints
  priority?: Priority
  executionPolicy?: ExecutionPolicy
  io?: TaskIO
}

export interface ExecutionAttempt {
  attemptNumber: number
  workerId: string
  startedAt: string
  completedAt?: string
  exitCode: number
  actualResources?: ResourceProfile
  failureReason?: string
  workerCausedFailure: boolean
  executionId: string
}

export interface TaskEnvelope {
  id: string
  descriptor: TaskDescriptor
  submittedAt: string
  state: TaskState
  executionHistory: ExecutionAttempt[]
}

export interface TaskPageResponse {
  tasks: TaskEnvelope[]
  total: number
  offset: number
  limit: number
}

export interface BulkResultEntry {
  taskId: string
  success: boolean
  reason?: string
}

// Worker/Metrics types
export interface WorkerMetricsSnapshot {
  workerId: string
  cpuUsagePercent: number
  heapUsedMB: number
  heapMaxMB: number
  threadCount: number
  activeTaskCount: number
  completedTaskCount: number
  failedTaskCount: number
  avgExecutionDurationMs: number
  reportedAt: string
}

export interface WorkerMetricsBucket {
  bucketStart: string
  workerId: string
  avgCpuPercent: number
  avgHeapUsedMB: number
  avgHeapMaxMB: number
  avgThreadCount: number
  avgActiveTaskCount: number
  avgCompletedTaskCount: number
  avgFailedTaskCount: number
  avgExecutionDurationMs: number
}

export interface ClusterMetrics {
  avgCpuPercent: number
  totalActiveTaskCount: number
  totalHeapUsedMB: number
  throughputPerMinute: number
  avgQueueWaitMs: number
  avgExecutionDurationMs: number
  workerCount: number
  healthyWorkerCount: number
}

// Scaling types
export interface ScalingPolicy {
  minWorkers: number
  maxWorkers: number
  cooldownSeconds: number
  scaleUpStep: number
  scaleDownStep: number
  drainTimeSeconds: number
}

export interface ScalingDecision {
  action: ScalingAction
  triggerType: string
  reason: string
  previousWorkerCount: number
  newWorkerCount: number
  timestamp: string
}

export interface ScalingStatusResponse {
  workerCount: number
  activeWorkerCount: number
  drainingWorkerCount: number
  policy: ScalingPolicy
  lastDecision?: ScalingDecision
  cooldownRemainingSeconds: number
  runtimeMode: string
}

// Auth types
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface LoginRequest {
  username: string
  password: string
}

// WebSocket message types
export type WsMessage =
  | { type: "INITIAL_SNAPSHOT"; payload: { workers: Array<{ workerId: string; healthState: WorkerHealthState; activeTaskCount: number }>; activeTaskCount: number; queuedTaskCount: number } }
  | { type: "TASK_UPDATE"; payload: TaskEnvelope }
  | { type: "WORKER_UPDATE"; payload: WorkerMetricsSnapshot }
  | { type: "WORKER_STATE"; payload: { workerId: string; state: WorkerHealthState } }
  | { type: "SCALING_EVENT"; payload: ScalingDecision }
  | { type: "ALERT"; payload: { severity: string; message: string; timestamp: string } }

// Strategy types
export interface StrategyResponse {
  strategy: string
  weights?: Record<string, number>
}

// Task logs
export interface TaskLogsResponse {
  stdout: string
  stderr: string
}
