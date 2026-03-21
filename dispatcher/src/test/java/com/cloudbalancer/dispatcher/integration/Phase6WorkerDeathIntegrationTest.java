package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.ChaosMonkeyService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.kafka.KafkaContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Phase 6 worker death recovery.
 * Starts 2 workers, submits tasks, kills one worker via ChaosMonkeyService,
 * and verifies that orphaned tasks are re-queued and eventually completed
 * by the surviving worker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "cloudbalancer.retry.scan-interval-ms=1000",
                "cloudbalancer.retry.base-delay-seconds=1",
                "cloudbalancer.dispatcher.heartbeat-suspect-threshold-seconds=60",
                "cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds=120"
        }
)
@Import(TestContainersConfig.class)
class Phase6WorkerDeathIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private ChaosMonkeyService chaosMonkeyService;

    @LocalServerPort
    private int port;

    private RestClient restClient;
    private TestWorkerSimulator worker1;
    private TestWorkerSimulator worker2;

    @BeforeEach
    void setUp() throws Exception {
        var baseClient = RestClient.create("http://localhost:" + port);

        // Login as seed admin to get access token
        String loginResponse = baseClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "admin", "password", "admin"))
                .retrieve()
                .body(String.class);
        String accessToken = JsonUtil.mapper()
                .readTree(loginResponse).get("accessToken").asText();

        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();

        // Start two workers
        worker1 = new TestWorkerSimulator("death-test-worker-1", kafka.getBootstrapServers());
        worker2 = new TestWorkerSimulator("death-test-worker-2", kafka.getBootstrapServers());
        worker1.start();
        worker2.start();
        Thread.sleep(3000); // allow registration to propagate
    }

    @AfterEach
    void tearDown() {
        if (worker1 != null) worker1.close();
        if (worker2 != null) worker2.close();
    }

    @Test
    void workerDeathRequeuesTasksToSurvivor() {
        // Submit several tasks that take a while to execute (1s each)
        List<UUID> taskIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var descriptor = new TaskDescriptor(
                    ExecutorType.SIMULATED,
                    Map.of("durationMs", 1000, "failProbability", 0.0),
                    new ResourceProfile(1, 512, 256, false, 10, false),
                    TaskConstraints.unconstrained(),
                    Priority.NORMAL,
                    ExecutionPolicy.defaults(),
                    TaskIO.none()
            );

            TaskEnvelope created = restClient.post()
                    .uri("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(descriptor)
                    .retrieve()
                    .body(TaskEnvelope.class);
            taskIds.add(created.getId());
        }

        // Wait for at least some tasks to be assigned
        await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            long assignedOrRunning = taskIds.stream()
                    .map(id -> restClient.get()
                            .uri("/api/tasks/{id}", id)
                            .retrieve()
                            .body(TaskEnvelope.class))
                    .filter(t -> t.getState() == TaskState.ASSIGNED ||
                                 t.getState() == TaskState.PROVISIONING ||
                                 t.getState() == TaskState.RUNNING ||
                                 t.getState() == TaskState.COMPLETED)
                    .count();
            assertThat(assignedOrRunning).isGreaterThanOrEqualTo(1);
        });

        // Kill worker-1 via ChaosMonkeyService
        chaosMonkeyService.killWorker(Optional.of("death-test-worker-1"));

        // Wait for all tasks to reach a terminal state (COMPLETED or DEAD_LETTERED)
        // The surviving worker-2 should pick up re-queued tasks
        await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            for (UUID id : taskIds) {
                TaskEnvelope fetched = restClient.get()
                        .uri("/api/tasks/{id}", id)
                        .retrieve()
                        .body(TaskEnvelope.class);
                assertThat(fetched.getState())
                        .as("Task %s should be in a terminal state", id)
                        .isIn(TaskState.COMPLETED, TaskState.DEAD_LETTERED, TaskState.FAILED);
            }
        });

        // Verify at least some tasks completed successfully on the surviving worker
        long completedCount = taskIds.stream()
                .map(id -> restClient.get()
                        .uri("/api/tasks/{id}", id)
                        .retrieve()
                        .body(TaskEnvelope.class))
                .filter(t -> t.getState() == TaskState.COMPLETED)
                .count();
        assertThat(completedCount).as("At least some tasks should complete on the surviving worker")
                .isGreaterThanOrEqualTo(1);
    }
}
