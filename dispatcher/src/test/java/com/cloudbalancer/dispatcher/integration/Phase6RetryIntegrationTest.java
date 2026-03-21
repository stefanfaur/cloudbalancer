package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Phase 6 retry and dead-lettering logic.
 * Submits a task with failProbability=1.0 and maxRetries=2, then verifies
 * the task eventually reaches DEAD_LETTERED state after exhausting retries.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "cloudbalancer.retry.scan-interval-ms=1000",
                "cloudbalancer.retry.base-delay-seconds=1"
        }
)
@Import(TestContainersConfig.class)
class Phase6RetryIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @LocalServerPort
    private int port;

    private RestClient restClient;
    private TestWorkerSimulator worker;

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

        worker = new TestWorkerSimulator("retry-test-worker-1", kafka.getBootstrapServers());
        worker.start();
        Thread.sleep(2000); // allow registration to propagate
    }

    @AfterEach
    void tearDown() {
        if (worker != null) worker.close();
    }

    @Test
    void taskFailsAndIsEventuallyDeadLettered() {
        // Submit a task that always fails, with maxRetries=2 and short backoff
        var descriptor = new TaskDescriptor(
                ExecutorType.SIMULATED,
                Map.of("durationMs", 100, "failProbability", 1.0),
                new ResourceProfile(1, 512, 256, false, 10, false),
                TaskConstraints.unconstrained(),
                Priority.NORMAL,
                new ExecutionPolicy(2, 300, BackoffStrategy.FIXED, FailureAction.RETRY),
                TaskIO.none()
        );

        TaskEnvelope created = restClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .body(descriptor)
                .retrieve()
                .body(TaskEnvelope.class);

        assertThat(created).isNotNull();
        assertThat(created.getState()).isEqualTo(TaskState.QUEUED);

        // Wait for the task to reach DEAD_LETTERED state after exhausting retries.
        // With maxRetries=2, base-delay=1s, scan-interval=1s, this should take ~30s max.
        await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            TaskEnvelope fetched = restClient.get()
                    .uri("/api/tasks/{id}", created.getId())
                    .retrieve()
                    .body(TaskEnvelope.class);
            assertThat(fetched).isNotNull();
            assertThat(fetched.getState()).isEqualTo(TaskState.DEAD_LETTERED);
        });

        // Verify execution history has multiple attempts
        TaskEnvelope finalState = restClient.get()
                .uri("/api/tasks/{id}", created.getId())
                .retrieve()
                .body(TaskEnvelope.class);
        assertThat(finalState.getExecutionHistory()).hasSizeGreaterThanOrEqualTo(2);
    }
}
