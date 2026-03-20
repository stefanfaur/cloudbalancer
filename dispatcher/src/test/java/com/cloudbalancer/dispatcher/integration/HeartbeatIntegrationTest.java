package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
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
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for HeartbeatTracker and HeartbeatListener.
 * Uses short thresholds (2s suspect, 4s dead) via test properties
 * to avoid long waits in tests.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "cloudbalancer.dispatcher.heartbeat-suspect-threshold-seconds=2",
                "cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds=4",
                "cloudbalancer.dispatcher.liveness-check-interval-ms=500"
        }
)
@Import(TestContainersConfig.class)
class HeartbeatIntegrationTest {

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private WorkerRepository workerRepository;

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
        workerRepository.deleteAll();
        if (producer != null) producer.close();
    }

    @Test
    void heartbeatViaKafkaSetsWorkerHealthy() throws Exception {
        String workerId = "hb-test-worker-" + UUID.randomUUID().toString().substring(0, 8);

        // First register the worker via Kafka
        var caps = new WorkerCapabilities(
                Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 8192, 10240, false, 0, true),
                Set.of()
        );
        var regEvent = new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(), workerId, caps);
        producer.send(new ProducerRecord<>("workers.registration", workerId,
                JsonUtil.mapper().writeValueAsString(regEvent))).get();

        // Wait for registration to be processed
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                workerRepository.findById(workerId).isPresent());

        // Now send a heartbeat event
        var heartbeatEvent = new WorkerHeartbeatEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, WorkerHealthState.HEALTHY);
        producer.send(new ProducerRecord<>("workers.heartbeat", workerId,
                JsonUtil.mapper().writeValueAsString(heartbeatEvent))).get();

        // Wait a moment for heartbeat to be processed, then verify worker is still HEALTHY
        // The heartbeat should prevent any liveness transition
        Thread.sleep(1000);

        WorkerRecord worker = workerRepository.findById(workerId).orElseThrow();
        assertThat(worker.getHealthState()).isEqualTo(WorkerHealthState.HEALTHY);
    }

    @Test
    void livenessTransitionsPersistedToSuspect() throws Exception {
        String workerId = "liveness-test-worker-" + UUID.randomUUID().toString().substring(0, 8);

        // Register worker via Kafka
        var caps = new WorkerCapabilities(
                Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 8192, 10240, false, 0, true),
                Set.of()
        );
        var regEvent = new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(), workerId, caps);
        producer.send(new ProducerRecord<>("workers.registration", workerId,
                JsonUtil.mapper().writeValueAsString(regEvent))).get();

        // Wait for registration
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                workerRepository.findById(workerId).isPresent());

        // Send one heartbeat so the tracker has an entry, then stop sending
        var heartbeatEvent = new WorkerHeartbeatEvent(
                UUID.randomUUID().toString(), Instant.now(), workerId, WorkerHealthState.HEALTHY);
        producer.send(new ProducerRecord<>("workers.heartbeat", workerId,
                JsonUtil.mapper().writeValueAsString(heartbeatEvent))).get();

        // Wait briefly for heartbeat to be consumed
        Thread.sleep(1000);

        // With suspect threshold of 2s and liveness check interval of 500ms,
        // worker should transition to SUSPECT within ~3-4 seconds after the last heartbeat
        await().atMost(10, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
            WorkerRecord w = workerRepository.findById(workerId).orElse(null);
            return w != null && w.getHealthState() != WorkerHealthState.HEALTHY;
        });

        WorkerRecord worker = workerRepository.findById(workerId).orElseThrow();
        assertThat(worker.getHealthState()).isIn(WorkerHealthState.SUSPECT, WorkerHealthState.DEAD);
    }
}
