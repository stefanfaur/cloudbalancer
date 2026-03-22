package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.executor.*;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String workerId;
    private final Map<ExecutorType, TaskExecutor> executors;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final CircuitBreaker circuitBreaker;
    private final WorkerChaosService workerChaosService;
    private final ArtifactService artifactService;

    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicLong completedTaskCount = new AtomicLong(0);
    private final AtomicLong failedTaskCount = new AtomicLong(0);
    private final AtomicLong totalExecutionDurationMs = new AtomicLong(0);
    private final AtomicLong executionCount = new AtomicLong(0);

    public TaskExecutionService(KafkaTemplate<String, String> kafkaTemplate,
                                 @Value("${cloudbalancer.worker.id:worker-1}") String workerId,
                                 @Qualifier("workerResultProducerCircuitBreaker") CircuitBreaker circuitBreaker,
                                 WorkerChaosService workerChaosService,
                                 List<TaskExecutor> executorList,
                                 ArtifactService artifactService) {
        this.kafkaTemplate = kafkaTemplate;
        this.workerId = workerId;
        this.circuitBreaker = circuitBreaker;
        this.workerChaosService = workerChaosService;
        this.executors = executorList.stream()
            .collect(Collectors.toMap(TaskExecutor::getExecutorType, e -> e));
        this.artifactService = artifactService;
    }

    public void executeTask(TaskAssignment assignment) {
        try {
            workerChaosService.checkAndApplyLatency("execution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Task execution interrupted by chaos latency injection");
            return;
        }

        TaskDescriptor descriptor = assignment.descriptor();
        TaskExecutor executor = executors.get(descriptor.executorType());
        if (executor == null) {
            publishResult(assignment.taskId(), new TaskResult(
                assignment.taskId(), workerId, 1, "",
                "No executor for type: " + descriptor.executorType(), 0, false, Instant.now(),
                assignment.executionId()
            ));
            return;
        }

        int timeoutSeconds = descriptor.executionPolicy() != null
            ? descriptor.executionPolicy().timeoutSeconds() : 300;

        var allocation = new ResourceAllocation(
            descriptor.resourceProfile() != null ? descriptor.resourceProfile().cpuCores() : 1,
            descriptor.resourceProfile() != null ? descriptor.resourceProfile().memoryMB() : 512,
            descriptor.resourceProfile() != null ? descriptor.resourceProfile().diskMB() : 256
        );

        // Create temp working directory
        Path workDir;
        try {
            workDir = Files.createTempDirectory("cb-task-");
        } catch (IOException e) {
            failedTaskCount.incrementAndGet();
            publishResult(assignment.taskId(), new TaskResult(
                assignment.taskId(), workerId, 1, "",
                "Failed to create working directory: " + e.getMessage(), 0, false, Instant.now(),
                assignment.executionId()
            ));
            return;
        }

        try {
            // Stage input artifacts
            TaskIO io = descriptor.io();
            List<TaskIO.InputArtifact> inputs = (io != null && io.inputs() != null) ? io.inputs() : Collections.emptyList();
            List<TaskIO.OutputArtifact> outputs = (io != null && io.outputs() != null) ? io.outputs() : Collections.emptyList();

            try {
                artifactService.stageInputs(inputs, workDir);
            } catch (Exception e) {
                failedTaskCount.incrementAndGet();
                publishResult(assignment.taskId(), new TaskResult(
                    assignment.taskId(), workerId, 1, "",
                    "Failed to stage input artifacts: " + e.getMessage(), 0, false, Instant.now(),
                    assignment.executionId()
                ));
                return;
            }

            // Create log callback and task context
            LogCallback logCallback = createLogCallback(assignment.taskId());
            var context = new TaskContext(assignment.taskId(), workDir, logCallback);

            Future<ExecutionResult> future = threadPool.submit(() ->
                executor.execute(descriptor.executionSpec(), allocation, context)
            );

            activeTaskCount.incrementAndGet();
            try {
                ExecutionResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);

                // Collect and upload output artifacts
                collectAndUploadArtifacts(assignment.taskId(), outputs, workDir);

                publishResult(assignment.taskId(), new TaskResult(
                    assignment.taskId(), workerId, result.exitCode(),
                    result.stdout(), result.stderr(), result.durationMs(),
                    result.timedOut(), Instant.now(), assignment.executionId()
                ));
                if (result.succeeded()) {
                    completedTaskCount.incrementAndGet();
                    totalExecutionDurationMs.addAndGet(result.durationMs());
                    executionCount.incrementAndGet();
                } else {
                    failedTaskCount.incrementAndGet();
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                executor.cancel(new ExecutionHandle(assignment.taskId().toString()));
                failedTaskCount.incrementAndGet();

                // Collect outputs even on failure
                collectAndUploadArtifacts(assignment.taskId(), outputs, workDir);

                publishResult(assignment.taskId(), new TaskResult(
                    assignment.taskId(), workerId, 1, "",
                    "Execution timed out after " + timeoutSeconds + "s",
                    timeoutSeconds * 1000L, true, Instant.now(), assignment.executionId()
                ));
            } catch (Exception e) {
                failedTaskCount.incrementAndGet();

                // Collect outputs even on failure
                collectAndUploadArtifacts(assignment.taskId(), outputs, workDir);

                publishResult(assignment.taskId(), new TaskResult(
                    assignment.taskId(), workerId, 1, "",
                    "Execution error: " + e.getMessage(), 0, false, Instant.now(),
                    assignment.executionId()
                ));
            } finally {
                activeTaskCount.decrementAndGet();
            }
        } finally {
            // Clean up working directory
            cleanupWorkDir(workDir);
        }
    }

    public int getActiveTaskCount() { return activeTaskCount.get(); }

    public long getCompletedTaskCount() { return completedTaskCount.get(); }

    public long getFailedTaskCount() { return failedTaskCount.get(); }

    public double getAverageExecutionDurationMs() {
        long count = executionCount.get();
        return count == 0 ? 0.0 : (double) totalExecutionDurationMs.get() / count;
    }

    private LogCallback createLogCallback(UUID taskId) {
        return (line, isStderr, timestamp) -> {
            try {
                LogMessage msg = new LogMessage(taskId, line, isStderr, timestamp);
                String json = JsonUtil.mapper().writeValueAsString(msg);
                kafkaTemplate.send("tasks.logs", taskId.toString(), json);
            } catch (Exception e) {
                log.debug("Failed to publish log line for task {}: {}", taskId, e.getMessage());
            }
        };
    }

    private void collectAndUploadArtifacts(UUID taskId, List<TaskIO.OutputArtifact> outputs, Path workDir) {
        try {
            List<ArtifactService.CollectedArtifact> collected = artifactService.collectOutputs(outputs, workDir);
            if (!collected.isEmpty()) {
                artifactService.uploadArtifacts(taskId, collected);
            }
        } catch (Exception e) {
            log.warn("Failed to collect/upload artifacts for task {}: {}", taskId, e.getMessage());
        }
    }

    private void cleanupWorkDir(Path workDir) {
        try {
            if (Files.exists(workDir)) {
                try (var walk = Files.walk(workDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.debug("Failed to delete {}: {}", p, e.getMessage());
                            }
                        });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to clean up working directory {}: {}", workDir, e.getMessage());
        }
    }

    private void publishResult(UUID taskId, TaskResult result) {
        try {
            String json = JsonUtil.mapper().writeValueAsString(result);
            circuitBreaker.executeRunnable(() -> kafkaTemplate.send("tasks.results", taskId.toString(), json));
            log.info("Published result for task {}: exitCode={}, timedOut={}", taskId, result.exitCode(), result.timedOut());
        } catch (CallNotPermittedException e) {
            log.error("Circuit breaker is open — cannot publish result for task {}: {}", taskId, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task result", e);
        }
    }
}
