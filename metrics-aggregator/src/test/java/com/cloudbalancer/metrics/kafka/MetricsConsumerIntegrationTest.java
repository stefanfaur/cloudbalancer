package com.cloudbalancer.metrics.kafka;

import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.event.TaskSubmittedEvent;
import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.TaskDescriptor;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.metrics.persistence.TaskMetricsRecord;
import com.cloudbalancer.metrics.persistence.TaskMetricsRepository;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRecord;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRecord;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRepository;
import com.cloudbalancer.metrics.test.TestContainersConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
class MetricsConsumerIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private WorkerMetricsRepository workerMetricsRepository;

    @Autowired
    private WorkerHeartbeatRepository workerHeartbeatRepository;

    @Autowired
    private TaskMetricsRepository taskMetricsRepository;

    private KafkaProducer<String, String> producer;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDown() {
        workerMetricsRepository.deleteAll();
        workerHeartbeatRepository.deleteAll();
        taskMetricsRepository.deleteAll();
        if (producer != null) producer.close();
    }

    @Test
    void workerMetricsStored() throws Exception {
        String workerId = "metrics-worker-" + UUID.randomUUID().toString().substring(0, 8);
        Instant reportedAt = Instant.now();

        var metrics = new WorkerMetrics(
                75.5, 1024, 2048, 42, 3, 100L, 5L, 250.0, reportedAt
        );
        var event = new WorkerMetricsEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, metrics
        );

        producer.send(new ProducerRecord<>("workers.metrics", workerId,
                JsonUtil.mapper().writeValueAsString(event))).get();

        await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> workerMetricsRepository.count() >= 1);

        List<WorkerMetricsRecord> records = workerMetricsRepository.findAll();
        assertThat(records).hasSize(1);

        WorkerMetricsRecord record = records.get(0);
        assertThat(record.getWorkerId()).isEqualTo(workerId);
        assertThat(record.getCpuUsagePercent()).isEqualTo(75.5);
        assertThat(record.getHeapUsedMB()).isEqualTo(1024);
        assertThat(record.getHeapMaxMB()).isEqualTo(2048);
        assertThat(record.getThreadCount()).isEqualTo(42);
        assertThat(record.getActiveTaskCount()).isEqualTo(3);
        assertThat(record.getCompletedTaskCount()).isEqualTo(100L);
        assertThat(record.getFailedTaskCount()).isEqualTo(5L);
        assertThat(record.getAvgExecutionDurationMs()).isEqualTo(250.0);
        assertThat(record.getReportedAt()).isEqualTo(reportedAt);
    }

    @Test
    void heartbeatStored() throws Exception {
        String workerId = "hb-worker-" + UUID.randomUUID().toString().substring(0, 8);
        Instant timestamp = Instant.now();

        var event = new WorkerHeartbeatEvent(
                UUID.randomUUID().toString(), timestamp, workerId, WorkerHealthState.HEALTHY
        );

        producer.send(new ProducerRecord<>("workers.heartbeat", workerId,
                JsonUtil.mapper().writeValueAsString(event))).get();

        await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> workerHeartbeatRepository.count() >= 1);

        List<WorkerHeartbeatRecord> records = workerHeartbeatRepository.findAll();
        assertThat(records).hasSize(1);

        WorkerHeartbeatRecord record = records.get(0);
        assertThat(record.getWorkerId()).isEqualTo(workerId);
        assertThat(record.getHealthState()).isEqualTo("HEALTHY");
    }

    @Test
    void taskCompletedEventStored() throws Exception {
        UUID taskId = UUID.randomUUID();
        Instant completedAt = Instant.now();

        var event = new TaskCompletedEvent(
                UUID.randomUUID().toString(), completedAt, taskId, 0, "output", ""
        );

        producer.send(new ProducerRecord<>("tasks.events", taskId.toString(),
                JsonUtil.mapper().writeValueAsString(event))).get();

        await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> taskMetricsRepository.count() >= 1);

        List<TaskMetricsRecord> records = taskMetricsRepository.findAll();
        assertThat(records).hasSize(1);

        TaskMetricsRecord record = records.get(0);
        assertThat(record.getTaskId()).isEqualTo(taskId);
        assertThat(record.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void multipleMetricsSnapshotsStored() throws Exception {
        String workerId = "multi-worker-" + UUID.randomUUID().toString().substring(0, 8);

        for (int i = 0; i < 5; i++) {
            Instant reportedAt = Instant.now().plusSeconds(i);
            var metrics = new WorkerMetrics(
                    50.0 + i, 512, 2048, 20, 1, i, 0L, 100.0, reportedAt
            );
            var event = new WorkerMetricsEvent(
                    UUID.randomUUID().toString(), Instant.now(), workerId, metrics
            );
            producer.send(new ProducerRecord<>("workers.metrics", workerId,
                    JsonUtil.mapper().writeValueAsString(event))).get();
        }

        await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> workerMetricsRepository.count() >= 5);

        List<WorkerMetricsRecord> records = workerMetricsRepository.findAll();
        assertThat(records).hasSize(5);
        assertThat(records).allMatch(r -> r.getWorkerId().equals(workerId));
    }

    @Test
    void unknownEventTypeIgnored() throws Exception {
        long initialCount = taskMetricsRepository.count();

        // TaskSubmittedEvent is not handled by TaskEventsListener
        var descriptor = new TaskDescriptor(
                ExecutorType.SIMULATED, Map.of("durationMs", 1000), null, null, null, null, null
        );
        var event = new TaskSubmittedEvent(
                UUID.randomUUID().toString(), Instant.now(), UUID.randomUUID(), descriptor
        );

        producer.send(new ProducerRecord<>("tasks.events", event.taskId().toString(),
                JsonUtil.mapper().writeValueAsString(event))).get();

        // Wait a bit to allow consumption, then verify no new rows
        Thread.sleep(3000);
        assertThat(taskMetricsRepository.count()).isEqualTo(initialCount);
    }
}
