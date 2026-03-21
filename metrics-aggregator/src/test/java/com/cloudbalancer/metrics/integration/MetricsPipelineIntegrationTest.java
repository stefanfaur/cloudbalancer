package com.cloudbalancer.metrics.integration;

import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.metrics.persistence.TaskMetricsRepository;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRepository;
import com.cloudbalancer.metrics.security.JwtService;
import com.cloudbalancer.metrics.test.TestContainersConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class MetricsPipelineIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WorkerMetricsRepository workerMetricsRepository;

    @Autowired
    private WorkerHeartbeatRepository workerHeartbeatRepository;

    @Autowired
    private TaskMetricsRepository taskMetricsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private String jwt() {
        return jwtService.generateAccessToken("testuser", Role.ADMIN);
    }

    @AfterEach
    void cleanup() {
        workerMetricsRepository.deleteAll();
        workerHeartbeatRepository.deleteAll();
        taskMetricsRepository.deleteAll();
    }

    // ---- Test 1: Metrics pipeline end-to-end ----

    @Test
    void metricsPipelineEndToEnd_publishEventAndQueryViaApi() throws Exception {
        String workerId = "e2e-worker-" + UUID.randomUUID().toString().substring(0, 8);
        Instant reportedAt = Instant.now();

        var metrics = new WorkerMetrics(
                72.5, 1024, 2048, 40, 5, 200L, 10L, 150.0, reportedAt
        );
        var event = new WorkerMetricsEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, metrics
        );

        kafkaTemplate.send("workers.metrics", workerId,
                JsonUtil.mapper().writeValueAsString(event));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    MvcResult result = mockMvc.perform(get("/api/metrics/workers")
                                    .header("Authorization", "Bearer " + jwt()))
                            .andExpect(status().isOk())
                            .andReturn();

                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(body.isArray()).isTrue();

                    JsonNode workerNode = findByWorkerId(body, workerId);
                    assertThat(workerNode).isNotNull();
                    assertThat(workerNode.get("cpuUsagePercent").asDouble()).isEqualTo(72.5);
                    assertThat(workerNode.get("heapUsedMB").asLong()).isEqualTo(1024);
                    assertThat(workerNode.get("heapMaxMB").asLong()).isEqualTo(2048);
                    assertThat(workerNode.get("threadCount").asInt()).isEqualTo(40);
                    assertThat(workerNode.get("activeTaskCount").asInt()).isEqualTo(5);
                    assertThat(workerNode.get("completedTaskCount").asLong()).isEqualTo(200L);
                    assertThat(workerNode.get("failedTaskCount").asLong()).isEqualTo(10L);
                    assertThat(workerNode.get("avgExecutionDurationMs").asDouble()).isEqualTo(150.0);
                });
    }

    // ---- Test 2: Heartbeat pipeline end-to-end ----

    @Test
    void heartbeatPipelineEndToEnd_publishEventAndVerifyInDb() throws Exception {
        String workerId = "hb-e2e-worker-" + UUID.randomUUID().toString().substring(0, 8);

        var event = new WorkerHeartbeatEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, WorkerHealthState.HEALTHY
        );

        kafkaTemplate.send("workers.heartbeat", workerId,
                JsonUtil.mapper().writeValueAsString(event));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var records = workerHeartbeatRepository.findAll().stream()
                            .filter(r -> r.getWorkerId().equals(workerId))
                            .toList();
                    assertThat(records).hasSize(1);
                    assertThat(records.get(0).getHealthState()).isEqualTo("HEALTHY");
                });
    }

    // ---- Test 3: Task metrics pipeline ----

    @Test
    void taskMetricsPipelineEndToEnd_publishEventAndQueryCluster() throws Exception {
        // First, publish a worker metrics event so cluster endpoint has data
        String workerId = "task-e2e-worker-" + UUID.randomUUID().toString().substring(0, 8);
        Instant reportedAt = Instant.now();

        var metrics = new WorkerMetrics(
                50.0, 512, 1024, 20, 2, 50L, 1L, 100.0, reportedAt
        );
        var metricsEvent = new WorkerMetricsEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, metrics
        );
        kafkaTemplate.send("workers.metrics", workerId,
                JsonUtil.mapper().writeValueAsString(metricsEvent));

        // Also send a heartbeat so cluster metrics has healthy count
        var heartbeatEvent = new WorkerHeartbeatEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, WorkerHealthState.HEALTHY
        );
        kafkaTemplate.send("workers.heartbeat", workerId,
                JsonUtil.mapper().writeValueAsString(heartbeatEvent));

        // Wait for worker metrics to land in DB first
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .until(() -> workerMetricsRepository.count() >= 1);

        // Publish a TaskCompletedEvent with completedAt = now (so it counts in throughput)
        UUID taskId = UUID.randomUUID();
        var taskEvent = new TaskCompletedEvent(
                UUID.randomUUID().toString(), Instant.now(), taskId, 0, "output", ""
        );
        kafkaTemplate.send("tasks.events", taskId.toString(),
                JsonUtil.mapper().writeValueAsString(taskEvent));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    MvcResult result = mockMvc.perform(get("/api/metrics/cluster")
                                    .header("Authorization", "Bearer " + jwt()))
                            .andExpect(status().isOk())
                            .andReturn();

                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(body.get("throughputPerMinute").asDouble()).isGreaterThan(0);
                });
    }

    // ---- Test 4: Time-range query end-to-end ----

    @Test
    void timeRangeQueryEndToEnd_publishMultipleEventsAndQueryHistory() throws Exception {
        String workerId = "history-worker-" + UUID.randomUUID().toString().substring(0, 8);
        Instant baseTime = Instant.now();

        // Publish 5 WorkerMetricsEvents at 1-second intervals
        for (int i = 0; i < 5; i++) {
            Instant reportedAt = baseTime.plusSeconds(i);
            var metrics = new WorkerMetrics(
                    50.0 + i * 5, 512, 2048, 20, 1 + i, (long) (10 + i), 0L, 100.0, reportedAt
            );
            var event = new WorkerMetricsEvent(
                    UUID.randomUUID().toString(), Instant.now(), workerId, metrics
            );
            kafkaTemplate.send("workers.metrics", workerId,
                    JsonUtil.mapper().writeValueAsString(event));
            Thread.sleep(1000);
        }

        // Wait for all 5 rows to land in DB
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .until(() -> workerMetricsRepository.findAll().stream()
                        .filter(r -> r.getWorkerId().equals(workerId))
                        .count() >= 5);

        // Query history with time range spanning all events
        String from = baseTime.minusSeconds(1).toString();
        String to = baseTime.plusSeconds(10).toString();

        MvcResult result = mockMvc.perform(get("/api/metrics/workers/" + workerId + "/history")
                        .param("from", from)
                        .param("to", to)
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(5);

        // Verify data is ordered and contains expected cpu values
        for (int i = 0; i < 5; i++) {
            JsonNode row = body.get(i);
            assertThat(row.get("workerId").asText()).isEqualTo(workerId);
            assertThat(row.get("avgCpuPercent").asDouble()).isEqualTo(50.0 + i * 5);
        }
    }

    // ---- Test 5: Multiple workers in cluster metrics ----

    @Test
    void multipleWorkersInClusterMetrics_publishMetricsFor3Workers() throws Exception {
        String[] workerIds = {
                "cluster-w1-" + UUID.randomUUID().toString().substring(0, 8),
                "cluster-w2-" + UUID.randomUUID().toString().substring(0, 8),
                "cluster-w3-" + UUID.randomUUID().toString().substring(0, 8)
        };

        for (String workerId : workerIds) {
            Instant reportedAt = Instant.now();
            var metrics = new WorkerMetrics(
                    60.0, 512, 2048, 30, 2, 100L, 5L, 200.0, reportedAt
            );
            var metricsEvent = new WorkerMetricsEvent(
                    UUID.randomUUID().toString(), Instant.now(), workerId, metrics
            );
            kafkaTemplate.send("workers.metrics", workerId,
                    JsonUtil.mapper().writeValueAsString(metricsEvent));

            var heartbeatEvent = new WorkerHeartbeatEvent(
                    UUID.randomUUID().toString(), Instant.now(), workerId, WorkerHealthState.HEALTHY
            );
            kafkaTemplate.send("workers.heartbeat", workerId,
                    JsonUtil.mapper().writeValueAsString(heartbeatEvent));
        }

        // Wait for all 3 workers to have metrics in DB
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    long distinctWorkers = workerMetricsRepository.findAll().stream()
                            .map(r -> r.getWorkerId())
                            .filter(id -> id.startsWith("cluster-w"))
                            .distinct()
                            .count();
                    return distinctWorkers >= 3;
                });

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    MvcResult result = mockMvc.perform(get("/api/metrics/cluster")
                                    .header("Authorization", "Bearer " + jwt()))
                            .andExpect(status().isOk())
                            .andReturn();

                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(body.get("workerCount").asInt()).isEqualTo(3);
                    assertThat(body.get("healthyWorkerCount").asInt()).isEqualTo(3);
                    assertThat(body.get("avgCpuPercent").asDouble()).isCloseTo(60.0,
                            org.assertj.core.data.Offset.offset(0.1));
                });
    }

    // ---- Helper ----

    private JsonNode findByWorkerId(JsonNode array, String workerId) {
        for (JsonNode node : array) {
            if (node.get("workerId").asText().equals(workerId)) {
                return node;
            }
        }
        return null;
    }
}
