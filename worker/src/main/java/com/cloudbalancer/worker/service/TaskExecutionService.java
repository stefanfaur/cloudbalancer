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
import java.nio.file.Path;
import java.time.Instant;
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

    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicLong completedTaskCount = new AtomicLong(0);
    private final AtomicLong failedTaskCount = new AtomicLong(0);
    private final AtomicLong totalExecutionDurationMs = new AtomicLong(0);
    private final AtomicLong executionCount = new AtomicLong(0);

    public TaskExecutionService(KafkaTemplate<String, String> kafkaTemplate,
                                 @Value("${cloudbalancer.worker.id:worker-1}") String workerId,
                                 @Qualifier("workerResultProducerCircuitBreaker") CircuitBreaker circuitBreaker,
                                 WorkerChaosService workerChaosService,
                                 List<TaskExecutor> executorList) {
        this.kafkaTemplate = kafkaTemplate;
        this.workerId = workerId;
        this.circuitBreaker = circuitBreaker;
        this.workerChaosService = workerChaosService;
        this.executors = executorList.stream()
            .collect(Collectors.toMap(TaskExecutor::getExecutorType, e -> e));
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
        var context = new TaskContext(assignment.taskId(), Path.of(System.getProperty("java.io.tmpdir")));

        Future<ExecutionResult> future = threadPool.submit(() ->
            executor.execute(descriptor.executionSpec(), allocation, context)
        );

        activeTaskCount.incrementAndGet();
        try {
            ExecutionResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
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
            publishResult(assignment.taskId(), new TaskResult(
                assignment.taskId(), workerId, 1, "",
                "Execution timed out after " + timeoutSeconds + "s",
                timeoutSeconds * 1000L, true, Instant.now(), assignment.executionId()
            ));
        } catch (Exception e) {
            failedTaskCount.incrementAndGet();
            publishResult(assignment.taskId(), new TaskResult(
                assignment.taskId(), workerId, 1, "",
                "Execution error: " + e.getMessage(), 0, false, Instant.now(),
                assignment.executionId()
            ));
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    public int getActiveTaskCount() { return activeTaskCount.get(); }

    public long getCompletedTaskCount() { return completedTaskCount.get(); }

    public long getFailedTaskCount() { return failedTaskCount.get(); }

    public double getAverageExecutionDurationMs() {
        long count = executionCount.get();
        return count == 0 ? 0.0 : (double) totalExecutionDurationMs.get() / count;
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
