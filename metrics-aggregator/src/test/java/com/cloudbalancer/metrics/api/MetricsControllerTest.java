package com.cloudbalancer.metrics.api;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.metrics.persistence.TaskMetricsRecord;
import com.cloudbalancer.metrics.persistence.TaskMetricsRepository;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRecord;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRecord;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRepository;
import com.cloudbalancer.metrics.security.JwtService;
import com.cloudbalancer.metrics.test.TestContainersConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class MetricsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private WorkerMetricsRepository workerMetricsRepo;
    @Autowired private WorkerHeartbeatRepository heartbeatRepo;
    @Autowired private TaskMetricsRepository taskMetricsRepo;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String jwt() {
        return jwtService.generateAccessToken("testuser", Role.ADMIN);
    }

    @AfterEach
    void cleanup() {
        workerMetricsRepo.deleteAll();
        heartbeatRepo.deleteAll();
        taskMetricsRepo.deleteAll();
    }

    // ---- helpers ----

    private WorkerMetricsRecord metricsRow(String workerId, Instant reportedAt,
                                           double cpu, long heapUsed, int activeTasks) {
        var r = new WorkerMetricsRecord();
        r.setWorkerId(workerId);
        r.setReportedAt(reportedAt);
        r.setCpuUsagePercent(cpu);
        r.setHeapUsedMB(heapUsed);
        r.setHeapMaxMB(2048);
        r.setThreadCount(10);
        r.setActiveTaskCount(activeTasks);
        r.setCompletedTaskCount(100);
        r.setFailedTaskCount(5);
        r.setAvgExecutionDurationMs(200.0);
        return r;
    }

    private WorkerHeartbeatRecord heartbeatRow(String workerId, String state) {
        var h = new WorkerHeartbeatRecord();
        h.setWorkerId(workerId);
        h.setHealthState(state);
        h.setTimestamp(Instant.now());
        return h;
    }

    private TaskMetricsRecord taskRow(Instant completedAt, long queueWaitMs, long execDurationMs) {
        var t = new TaskMetricsRecord();
        t.setTaskId(UUID.randomUUID());
        t.setSubmittedAt(completedAt.minusMillis(queueWaitMs + execDurationMs));
        t.setAssignedAt(completedAt.minusMillis(execDurationMs));
        t.setStartedAt(completedAt.minusMillis(execDurationMs));
        t.setCompletedAt(completedAt);
        t.setQueueWaitMs(queueWaitMs);
        t.setExecutionDurationMs(execDurationMs);
        t.setTurnaroundMs(queueWaitMs + execDurationMs);
        return t;
    }

    // ---- Test 1: GET /api/metrics/workers — latest per worker ----
    @Test
    void getLatestMetrics_returnsLatestPerWorker() throws Exception {
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // worker-1: 3 rows at t=1s, t=2s, t=3s — latest should be t=3s with cpu=30
        workerMetricsRepo.save(metricsRow("worker-1", base.plusSeconds(1), 10.0, 512, 1));
        workerMetricsRepo.save(metricsRow("worker-1", base.plusSeconds(2), 20.0, 512, 2));
        workerMetricsRepo.save(metricsRow("worker-1", base.plusSeconds(3), 30.0, 512, 3));

        // worker-2: 2 rows at t=1s, t=2s — latest should be t=2s with cpu=25
        workerMetricsRepo.save(metricsRow("worker-2", base.plusSeconds(1), 15.0, 256, 1));
        workerMetricsRepo.save(metricsRow("worker-2", base.plusSeconds(2), 25.0, 256, 2));

        MvcResult result = mockMvc.perform(get("/api/metrics/workers")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(2);

        // Find worker-1 entry
        JsonNode w1 = findByWorkerId(body, "worker-1");
        assertThat(w1).isNotNull();
        assertThat(w1.get("cpuUsagePercent").asDouble()).isEqualTo(30.0);
        assertThat(w1.get("activeTaskCount").asInt()).isEqualTo(3);

        // Find worker-2 entry
        JsonNode w2 = findByWorkerId(body, "worker-2");
        assertThat(w2).isNotNull();
        assertThat(w2.get("cpuUsagePercent").asDouble()).isEqualTo(25.0);
        assertThat(w2.get("activeTaskCount").asInt()).isEqualTo(2);
    }

    // ---- Test 2: GET /api/metrics/workers — empty cluster ----
    @Test
    void getLatestMetrics_emptyCluster_returnsEmptyList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/metrics/workers")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(0);
    }

    // ---- Test 3: GET /api/metrics/workers/{id}/history — time range ----
    @Test
    void getWorkerHistory_timeRange_filtersCorrectly() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Insert 10 rows spanning 10 minutes (1 per minute)
        for (int i = 0; i < 10; i++) {
            workerMetricsRepo.save(metricsRow("worker-1",
                    now.minus(10 - i, ChronoUnit.MINUTES), 50.0 + i, 512, 1));
        }

        // Request from=5 minutes ago to now — should get rows at -5m, -4m, -3m, -2m, -1m, 0m = 6 rows
        String from = now.minus(5, ChronoUnit.MINUTES).toString();
        String to = now.toString();

        MvcResult result = mockMvc.perform(get("/api/metrics/workers/worker-1/history")
                        .param("from", from)
                        .param("to", to)
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        // Rows at: -5m, -4m, -3m, -2m, -1m, 0m = 6 rows
        assertThat(body.size()).isBetween(5, 6);
    }

    // ---- Test 4: GET /api/metrics/workers/{id}/history — bucket aggregation ----
    @Test
    void getWorkerHistory_bucketAggregation_returnsBuckets() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Insert 20 rows over 10 minutes (2 per minute)
        for (int i = 0; i < 20; i++) {
            Instant t = now.minus(10, ChronoUnit.MINUTES).plusSeconds(i * 30L);
            workerMetricsRepo.save(metricsRow("worker-1", t, 50.0 + i, 512, 1));
        }

        String from = now.minus(10, ChronoUnit.MINUTES).toString();
        String to = now.toString();

        MvcResult result = mockMvc.perform(get("/api/metrics/workers/worker-1/history")
                        .param("from", from)
                        .param("to", to)
                        .param("bucket", "5m")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        // TimescaleDB time_bucket aligns to epoch boundaries, so 2 or 3 buckets depending on alignment
        assertThat(body.size()).isBetween(2, 3);

        // Each bucket should have averaged values
        for (JsonNode bucket : body) {
            assertThat(bucket.has("bucketStart")).isTrue();
            assertThat(bucket.has("avgCpuPercent")).isTrue();
            assertThat(bucket.get("avgCpuPercent").asDouble()).isGreaterThan(0);
        }
    }

    // ---- Test 5: GET /api/metrics/workers/{id}/history — worker not found ----
    @Test
    void getWorkerHistory_nonExistentWorker_returnsEmptyList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/metrics/workers/worker-99/history")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(0);
    }

    // ---- Test 6: GET /api/metrics/cluster — aggregate values ----
    @Test
    void getClusterMetrics_aggregatesCorrectly() throws Exception {
        Instant now = Instant.now();

        // 3 workers with known metrics
        workerMetricsRepo.save(metricsRow("worker-1", now, 60.0, 512, 2));
        workerMetricsRepo.save(metricsRow("worker-2", now, 80.0, 256, 3));
        workerMetricsRepo.save(metricsRow("worker-3", now, 40.0, 128, 1));

        // Heartbeats — 2 healthy, 1 unhealthy
        heartbeatRepo.save(heartbeatRow("worker-1", "HEALTHY"));
        heartbeatRepo.save(heartbeatRow("worker-2", "HEALTHY"));
        heartbeatRepo.save(heartbeatRow("worker-3", "UNHEALTHY"));

        MvcResult result = mockMvc.perform(get("/api/metrics/cluster")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        // avgCpuPercent = (60+80+40)/3 = 60.0
        assertThat(body.get("avgCpuPercent").asDouble()).isCloseTo(60.0, org.assertj.core.data.Offset.offset(0.1));
        // totalActiveTaskCount = 2+3+1 = 6
        assertThat(body.get("totalActiveTaskCount").asInt()).isEqualTo(6);
        // totalHeapUsedMB = 512+256+128 = 896
        assertThat(body.get("totalHeapUsedMB").asLong()).isEqualTo(896);
        // workerCount = 3
        assertThat(body.get("workerCount").asInt()).isEqualTo(3);
        // healthyWorkerCount = 2
        assertThat(body.get("healthyWorkerCount").asInt()).isEqualTo(2);
    }

    // ---- Test 7: GET /api/metrics/cluster — includes task throughput ----
    @Test
    void getClusterMetrics_includesTaskThroughput() throws Exception {
        Instant now = Instant.now();

        // Need at least one worker metrics row for cluster metrics
        workerMetricsRepo.save(metricsRow("worker-1", now, 50.0, 512, 1));
        heartbeatRepo.save(heartbeatRow("worker-1", "HEALTHY"));

        // 5 tasks completed in the last minute
        for (int i = 0; i < 5; i++) {
            taskMetricsRepo.save(taskRow(now.minusSeconds(i * 10L), 100, 500));
        }

        MvcResult result = mockMvc.perform(get("/api/metrics/cluster")
                        .header("Authorization", "Bearer " + jwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        // throughputPerMinute should be 5 (5 tasks in last minute)
        assertThat(body.get("throughputPerMinute").asDouble()).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.1));
        // avgQueueWaitMs = 100
        assertThat(body.get("avgQueueWaitMs").asDouble()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
        // avgExecutionDurationMs = 500
        assertThat(body.get("avgExecutionDurationMs").asDouble()).isCloseTo(500.0, org.assertj.core.data.Offset.offset(0.1));
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
