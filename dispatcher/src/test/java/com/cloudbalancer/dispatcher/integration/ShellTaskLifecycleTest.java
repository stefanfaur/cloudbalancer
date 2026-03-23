package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.executor.ShellExecutor;
import com.cloudbalancer.common.executor.SimulatedExecutor;
import com.cloudbalancer.common.executor.TaskExecutor;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
class ShellTaskLifecycleTest {

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @LocalServerPort
    private int port;

    private RestClient restClient;
    private TestWorkerSimulator worker;

    @BeforeEach
    void setUp() throws Exception {
        // Clean stale state from other integration tests sharing this Spring context
        taskRepository.deleteAll();
        workerRepository.deleteAll();

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

        // Worker supports SHELL and SIMULATED (but NOT DOCKER)
        Map<ExecutorType, TaskExecutor> executorMap = Map.of(
            ExecutorType.SHELL, new ShellExecutor(),
            ExecutorType.SIMULATED, new SimulatedExecutor()
        );
        worker = new TestWorkerSimulator(
            "shell-integration-worker-1",
            kafka.getBootstrapServers(),
            Set.of(ExecutorType.SHELL, ExecutorType.SIMULATED),
            executorMap
        );
        worker.start();
        Thread.sleep(2000); // allow registration to propagate
    }

    @AfterEach
    void tearDown() {
        if (worker != null) worker.close();
    }

    @Test
    void shellTaskCompletesSuccessfully() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SHELL,
            Map.of("command", "echo integration-test"),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
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
    void dockerTaskStaysQueuedWhenNoDockerCapableWorkerExists() {
        var descriptor = new TaskDescriptor(
            ExecutorType.DOCKER,
            Map.of("image", "alpine", "command", List.of("echo", "test")),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
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

        // Wait a few seconds and verify the task stays QUEUED (no worker supports DOCKER)
        await().during(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
                TaskEnvelope fetched = restClient.get()
                    .uri("/api/tasks/{id}", created.getId())
                    .retrieve()
                    .body(TaskEnvelope.class);
                assertThat(fetched).isNotNull();
                assertThat(fetched.getState()).isEqualTo(TaskState.QUEUED);
            });
    }
}
