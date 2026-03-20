package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.api.dto.StrategyRequest;
import com.cloudbalancer.dispatcher.kafka.TaskResultListener;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.persistence.SchedulingConfigRepository;
import com.cloudbalancer.dispatcher.service.SchedulingConfigService;
import com.cloudbalancer.dispatcher.service.TaskAssignmentService;
import com.cloudbalancer.dispatcher.service.TaskService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
class Phase4SchedulingIntegrationTest {

    @Autowired TaskService taskService;
    @Autowired TaskAssignmentService assignmentService;
    @Autowired WorkerRegistryService workerRegistry;
    @Autowired TaskResultListener resultListener;
    @Autowired TaskRepository taskRepository;
    @Autowired WorkerRepository workerRepository;
    @Autowired SchedulingConfigRepository schedulingConfigRepository;
    @Autowired SchedulingConfigService configService;
    @Autowired KafkaContainer kafka;

    @LocalServerPort int port;

    private RestClient adminClient;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        workerRepository.deleteAll();
        schedulingConfigRepository.deleteAll();
        // Reset to ROUND_ROBIN
        configService.switchStrategy("ROUND_ROBIN", Map.of());

        var baseClient = RestClient.create("http://localhost:" + port);
        String loginResponse = baseClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", "admin", "password", "admin"))
            .retrieve()
            .body(String.class);
        String accessToken = JsonUtil.mapper()
            .readTree(loginResponse).get("accessToken").asText();
        adminClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    @Test
    void healthFilterExcludesDeadWorkers() {
        registerWorker("healthy-w", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        registerDeadWorker("dead-w", Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("healthy-w");
    }

    @Test
    void executorCapabilityFilterExcludesIncompatibleWorkers() {
        registerWorker("shell-only", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SHELL), 8, 4096, 1000);
        registerWorker("docker-shell", WorkerHealthState.HEALTHY, Set.of(ExecutorType.DOCKER, ExecutorType.SHELL), 8, 4096, 1000);
        submitTask(Priority.NORMAL, ExecutorType.DOCKER, 1, 256, 50);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("docker-shell");
    }

    @Test
    void resourceSufficiencyFilterExcludesOverloadedWorkers() {
        registerWorker("full-w", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 4, 2048, 500);
        // Fill up worker resources
        workerRegistry.allocateResources("full-w", new ResourceProfile(4, 2048, 500, false, 0, false));
        registerWorker("free-w", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);

        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 2, 512, 100);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("free-w");
    }

    @Test
    void constraintFilterEnforcesRequiredTags() {
        registerWorkerWithTags("gpu-w", Set.of("gpu-enabled"));
        registerWorkerWithTags("plain-w", Set.of());

        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100),
            new ResourceProfile(1, 256, 50, false, 10, false),
            new TaskConstraints(Set.of("gpu-enabled"), Set.of(), Set.of()),
            Priority.NORMAL, ExecutionPolicy.defaults(), TaskIO.none()
        );
        taskService.submitTask(descriptor);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("gpu-w");
    }

    @Test
    void taskStaysQueuedWhenNoEligibleWorker() {
        // No workers at all
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);

        assignmentService.assignPendingTasks();

        assertThat(taskRepository.findByState(TaskState.QUEUED)).hasSize(1);
        assertThat(taskRepository.findByState(TaskState.ASSIGNED)).isEmpty();
    }

    @Test
    void priorityOrderingCriticalBeforeLow() {
        // One worker with capacity for only 1 task
        registerWorker("w1", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 2, 512, 100);
        submitTask(Priority.LOW, ExecutorType.SIMULATED, 2, 512, 100);
        submitTask(Priority.CRITICAL, ExecutorType.SIMULATED, 2, 512, 100);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getPriority()).isEqualTo(Priority.CRITICAL);

        var queued = taskRepository.findByState(TaskState.QUEUED);
        assertThat(queued).hasSize(1);
        assertThat(queued.getFirst().getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void resourceLedgerTracksAcrossLifecycle() {
        registerWorker("w1", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);

        // Submit and assign 2 tasks
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 2, 512, 100);
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);
        assignmentService.assignPendingTasks();

        var worker = workerRepository.findById("w1").orElseThrow();
        assertThat(worker.getAllocatedCpu()).isEqualTo(3);
        assertThat(worker.getAllocatedMemoryMb()).isEqualTo(768);
        assertThat(worker.getActiveTaskCount()).isEqualTo(2);

        // Complete the first task
        var assignedTasks = taskRepository.findByState(TaskState.ASSIGNED);
        var firstTask = assignedTasks.getFirst();
        firstTask.transitionTo(TaskState.PROVISIONING);
        firstTask.transitionTo(TaskState.RUNNING);
        firstTask.transitionTo(TaskState.POST_PROCESSING);
        firstTask.transitionTo(TaskState.COMPLETED);
        taskRepository.save(firstTask);
        workerRegistry.releaseResources("w1", firstTask.getDescriptor().resourceProfile());

        worker = workerRepository.findById("w1").orElseThrow();
        assertThat(worker.getActiveTaskCount()).isEqualTo(1);
        // Remaining allocation is from the second task
        assertThat(worker.getAllocatedCpu()).isLessThanOrEqualTo(2);
    }

    @Test
    void strategySwitchViaAdminApi() {
        // Start with ROUND_ROBIN (default from setUp)
        var getResponse = adminClient.get()
            .uri("/api/admin/strategy")
            .retrieve()
            .body(String.class);
        assertThat(getResponse).contains("ROUND_ROBIN");

        // Switch to RESOURCE_FIT
        var switchResponse = adminClient.put()
            .uri("/api/admin/strategy")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new StrategyRequest("RESOURCE_FIT", null))
            .retrieve()
            .body(String.class);
        assertThat(switchResponse).contains("RESOURCE_FIT");

        // Verify resource-fit behavior: free worker gets task
        registerWorker("loaded", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        workerRegistry.allocateResources("loaded", new ResourceProfile(6, 3000, 800, false, 0, false));
        registerWorker("free", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);

        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);
        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("free");
    }

    @Test
    void customWeightsViaAdminApi() {
        // Switch to CUSTOM with 100% resource availability
        adminClient.put()
            .uri("/api/admin/strategy")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new StrategyRequest("CUSTOM", Map.of("resourceAvailability", 100, "queueDepth", 0)))
            .retrieve()
            .body(String.class);

        registerWorker("loaded", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        workerRegistry.allocateResources("loaded", new ResourceProfile(6, 3000, 800, false, 0, false));
        registerWorker("free", WorkerHealthState.HEALTHY, Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);

        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);
        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("free");
    }

    @Test
    void fullLifecycleWithKafkaAndPostgres() throws Exception {
        var worker = new TestWorkerSimulator("lifecycle-worker-1", kafka.getBootstrapServers());
        try {
            worker.start();
            Thread.sleep(2000); // allow registration

            // Submit task via REST
            var descriptor = new TaskDescriptor(
                ExecutorType.SIMULATED, Map.of("durationMs", 500, "failProbability", 0.0),
                new ResourceProfile(1, 512, 256, false, 10, false),
                TaskConstraints.unconstrained(), Priority.NORMAL,
                ExecutionPolicy.defaults(), TaskIO.none()
            );

            TaskEnvelope created = adminClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .body(descriptor)
                .retrieve()
                .body(TaskEnvelope.class);
            assertThat(created.getState()).isEqualTo(TaskState.QUEUED);

            // Wait for task to complete via Kafka round-trip
            await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                TaskEnvelope fetched = adminClient.get()
                    .uri("/api/tasks/{id}", created.getId())
                    .retrieve()
                    .body(TaskEnvelope.class);
                assertThat(fetched.getState()).isEqualTo(TaskState.COMPLETED);
            });

            // Verify task persisted in Postgres
            var record = taskRepository.findById(created.getId()).orElseThrow();
            assertThat(record.getState()).isEqualTo(TaskState.COMPLETED);
            assertThat(record.getAssignedWorkerId()).isEqualTo("lifecycle-worker-1");

            // Verify resource ledger released
            var workerRecord = workerRepository.findById("lifecycle-worker-1").orElseThrow();
            assertThat(workerRecord.getAllocatedCpu()).isEqualTo(0);
            assertThat(workerRecord.getActiveTaskCount()).isEqualTo(0);
        } finally {
            worker.close();
        }
    }

    // --- helpers ---

    private void registerWorker(String id, WorkerHealthState state,
                                 Set<ExecutorType> executors, int cpu, int mem, int disk) {
        workerRegistry.registerWorker(id, state,
            new WorkerCapabilities(executors,
                new ResourceProfile(cpu, mem, disk, false, 0, false), Set.of()));
    }

    private void registerDeadWorker(String id, Set<ExecutorType> executors, int cpu, int mem, int disk) {
        registerWorker(id, WorkerHealthState.DEAD, executors, cpu, mem, disk);
    }

    private void registerWorkerWithTags(String id, Set<String> tags) {
        workerRegistry.registerWorker(id, WorkerHealthState.HEALTHY,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(8, 4096, 1000, false, 0, false), tags));
    }

    private void submitTask(Priority priority, ExecutorType executorType, int cpu, int mem, int disk) {
        var descriptor = new TaskDescriptor(
            executorType, Map.of("durationMs", 100),
            new ResourceProfile(cpu, mem, disk, false, 10, false),
            TaskConstraints.unconstrained(), priority,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        taskService.submitTask(descriptor);
    }
}
