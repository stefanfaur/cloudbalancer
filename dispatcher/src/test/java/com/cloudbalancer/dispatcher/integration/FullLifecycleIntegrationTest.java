package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FullLifecycleIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.0")
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    private RestClient restClient;
    private TestWorkerSimulator worker;

    @BeforeEach
    void setUp() throws Exception {
        restClient = RestClient.create("http://localhost:" + port);
        worker = new TestWorkerSimulator("integration-worker-1", kafka.getBootstrapServers());
        worker.start();
        Thread.sleep(2000); // allow registration to propagate
    }

    @AfterEach
    void tearDown() {
        if (worker != null) worker.close();
    }

    @Test
    void submitTaskFlowsToCompletedViaWorker() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 500, "failProbability", 0.0),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );

        // Submit
        TaskEnvelope created = restClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .body(descriptor)
            .retrieve()
            .body(TaskEnvelope.class);

        assertThat(created).isNotNull();
        assertThat(created.getState()).isEqualTo(TaskState.QUEUED);

        // Wait for completion
        await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            TaskEnvelope fetched = restClient.get()
                .uri("/api/tasks/{id}", created.getId())
                .retrieve()
                .body(TaskEnvelope.class);
            assertThat(fetched).isNotNull();
            assertThat(fetched.getState()).isEqualTo(TaskState.COMPLETED);
        });
    }

    @Test
    void taskWithHighFailProbabilityReachesFailed() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100, "failProbability", 1.0),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );

        TaskEnvelope created = restClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .body(descriptor)
            .retrieve()
            .body(TaskEnvelope.class);

        await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            TaskEnvelope fetched = restClient.get()
                .uri("/api/tasks/{id}", created.getId())
                .retrieve()
                .body(TaskEnvelope.class);
            assertThat(fetched).isNotNull();
            assertThat(fetched.getState()).isEqualTo(TaskState.FAILED);
        });
    }

    @Test
    void multipleConcurrentTasksAllComplete() {
        int taskCount = 10;
        java.util.List<java.util.UUID> taskIds = new java.util.ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            var descriptor = new TaskDescriptor(
                ExecutorType.SIMULATED, Map.of("durationMs", 200, "failProbability", 0.0),
                new ResourceProfile(1, 512, 256, false, 10, false),
                TaskConstraints.unconstrained(), Priority.NORMAL,
                ExecutionPolicy.defaults(), TaskIO.none()
            );
            TaskEnvelope created = restClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .body(descriptor)
                .retrieve()
                .body(TaskEnvelope.class);
            taskIds.add(created.getId());
        }

        await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
            for (java.util.UUID id : taskIds) {
                TaskEnvelope fetched = restClient.get()
                    .uri("/api/tasks/{id}", id)
                    .retrieve()
                    .body(TaskEnvelope.class);
                assertThat(fetched.getState()).isIn(TaskState.COMPLETED, TaskState.FAILED);
            }
        });

        // All should be COMPLETED since failProbability=0.0
        for (java.util.UUID id : taskIds) {
            TaskEnvelope fetched = restClient.get()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .body(TaskEnvelope.class);
            assertThat(fetched.getState()).isEqualTo(TaskState.COMPLETED);
        }
    }
}
