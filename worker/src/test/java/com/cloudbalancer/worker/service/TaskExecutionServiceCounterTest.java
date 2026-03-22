package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.executor.SimulatedExecutor;
import com.cloudbalancer.common.model.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceCounterTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private CircuitBreaker circuitBreaker;
    @Mock
    private WorkerChaosService workerChaosService;
    @Mock
    private ArtifactService artifactService;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        // Make circuit breaker execute the runnable passed to it
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(circuitBreaker).executeRunnable(any(Runnable.class));
        service = new TaskExecutionService(kafkaTemplate, "test-worker", circuitBreaker, workerChaosService, List.of(new SimulatedExecutor()), artifactService);
    }

    private TaskAssignment createAssignment(int durationMs, double failProbability) {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", durationMs, "failProbability", failProbability),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        return new TaskAssignment(UUID.randomUUID(), descriptor, "test-worker", Instant.now(), UUID.randomUUID());
    }

    private TaskAssignment createAssignmentWithTimeout(int durationMs, double failProbability, int timeoutSeconds) {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", durationMs, "failProbability", failProbability),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            new ExecutionPolicy(3, timeoutSeconds, BackoffStrategy.FIXED, FailureAction.RETRY),
            TaskIO.none()
        );
        return new TaskAssignment(UUID.randomUUID(), descriptor, "test-worker", Instant.now(), UUID.randomUUID());
    }

    @Test
    void completedCounterIncrements() {
        service.executeTask(createAssignment(100, 0.0));
        assertThat(service.getCompletedTaskCount()).isEqualTo(1);

        service.executeTask(createAssignment(100, 0.0));
        assertThat(service.getCompletedTaskCount()).isEqualTo(2);
    }

    @Test
    void failedCounterIncrements() {
        service.executeTask(createAssignment(100, 1.0));
        assertThat(service.getFailedTaskCount()).isEqualTo(1);
    }

    @Test
    void activeTaskCount() throws Exception {
        // Use a long-running task so we can observe the active count while it runs
        CountDownLatch taskStarted = new CountDownLatch(1);

        // Submit a long-running task on a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            // We need to signal that the task started, but executeTask is blocking.
            // Use a short poll loop to detect when the active count goes up.
            service.executeTask(createAssignment(3000, 0.0));
        });

        // Poll briefly until activeTaskCount >= 1 (the task needs a moment to start)
        boolean sawActive = false;
        for (int i = 0; i < 50; i++) {
            if (service.getActiveTaskCount() >= 1) {
                sawActive = true;
                break;
            }
            Thread.sleep(50);
        }
        assertThat(sawActive).as("Should observe at least 1 active task while task runs").isTrue();

        // Wait for completion
        future.get(10, TimeUnit.SECONDS);
        assertThat(service.getActiveTaskCount()).isEqualTo(0);

        executor.shutdown();
    }

    @Test
    void averageExecutionDuration() {
        service.executeTask(createAssignment(100, 0.0));
        service.executeTask(createAssignment(100, 0.0));
        service.executeTask(createAssignment(100, 0.0));

        double avg = service.getAverageExecutionDurationMs();
        assertThat(avg).isBetween(80.0, 200.0);
    }

    @Test
    void countersAreThreadSafe() throws Exception {
        int taskCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch latch = new CountDownLatch(taskCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            // Mix of success and failure tasks; exact split is non-deterministic
            // but total must equal taskCount
            double failProb = (i % 3 == 0) ? 1.0 : 0.0;
            futures.add(executor.submit(() -> {
                try {
                    service.executeTask(createAssignment(100, failProb));
                } finally {
                    latch.countDown();
                }
            }));
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).as("All tasks should complete within timeout").isTrue();

        // Wait for all futures to ensure no exceptions were swallowed
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }

        long total = service.getCompletedTaskCount() + service.getFailedTaskCount();
        assertThat(total).isEqualTo(taskCount);

        executor.shutdown();
    }
}
