package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.event.CloudBalancerEvent;
import com.cloudbalancer.common.event.ScalingEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.service.AutoScalerService;
import com.cloudbalancer.dispatcher.service.ScalingPolicyService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "cloudbalancer.dispatcher.scaling.enabled=true"
    }
)
@Import(TestContainersConfig.class)
class AutoScalingLifecycleTest {

    @Autowired private WorkerRepository workerRepository;
    @Autowired private WorkerRegistryService workerRegistry;
    @Autowired private AutoScalerService autoScaler;
    @Autowired private ScalingPolicyService policyService;
    @Autowired private KafkaContainer kafka;

    private KafkaConsumer<String, String> scalingConsumer;

    @BeforeEach
    void setUp() {
        workerRepository.deleteAll();
        policyService.reloadPolicy();
        autoScaler.resetForTest();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-scaling-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        scalingConsumer = new KafkaConsumer<>(props);
        scalingConsumer.subscribe(List.of("system.scaling"));
    }

    @AfterEach
    void tearDown() {
        if (scalingConsumer != null) {
            scalingConsumer.close(Duration.ofSeconds(5));
        }
    }

    @Test
    void reactiveScaleUpLifecycle() {
        // Register 3 initial workers
        registerWorker("w1");
        registerWorker("w2");
        registerWorker("w3");

        // Feed high CPU metrics and trigger evaluation
        autoScaler.recordMetrics("w1", 90.0);
        autoScaler.recordMetrics("w2", 92.0);
        autoScaler.recordMetrics("w3", 88.0);

        autoScaler.evaluate();

        // Verify new worker appeared in DB
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var all = workerRepository.findAll();
            assertThat(all.size()).isGreaterThan(3);
        });

        // Verify lastDecision
        var decision = autoScaler.getLastDecision();
        assertThat(decision).isNotNull();
        assertThat(decision.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(decision.triggerType()).isEqualTo(ScalingTriggerType.REACTIVE);
        assertThat(decision.newWorkerCount()).isGreaterThan(decision.previousWorkerCount());
    }

    @Test
    void manualScaleUpAddsWorker() {
        registerWorker("w1");
        registerWorker("w2");

        var decision = autoScaler.triggerManual(ScalingAction.SCALE_UP, 1);

        assertThat(decision.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(decision.triggerType()).isEqualTo(ScalingTriggerType.MANUAL);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var all = workerRepository.findAll();
            assertThat(all.size()).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void scaleDownDrainsWorker() {
        // Set policy with minWorkers=1
        policyService.updatePolicy(new ScalingPolicy(1, 20, Duration.ofSeconds(1), 1, 1, Duration.ofSeconds(60)));

        registerWorker("w1");
        registerWorker("w2");
        registerWorker("w3");

        // Feed low CPU and set queue empty
        autoScaler.advanceWindowForTest(Duration.ofMinutes(6));
        autoScaler.recordMetrics("w1", 10.0);
        autoScaler.recordMetrics("w2", 8.0);
        autoScaler.recordMetrics("w3", 12.0);
        autoScaler.setQueueEmptySince(Instant.now().minus(Duration.ofMinutes(6)));

        autoScaler.evaluate();

        // Verify one worker is now DRAINING
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var draining = workerRepository.findByHealthState(WorkerHealthState.DRAINING);
            assertThat(draining).hasSize(1);
        });

        var decision = autoScaler.getLastDecision();
        assertThat(decision.action()).isEqualTo(ScalingAction.SCALE_DOWN);
    }

    @Test
    void respectsMaxWorkersBoundOnManualTrigger() {
        policyService.updatePolicy(new ScalingPolicy(2, 3, Duration.ofSeconds(1), 1, 1, Duration.ofSeconds(60)));

        registerWorker("w1");
        registerWorker("w2");
        registerWorker("w3");

        var decision = autoScaler.triggerManual(ScalingAction.SCALE_UP, 2);

        assertThat(decision.action()).isEqualTo(ScalingAction.NONE);
        assertThat(decision.reason()).contains("bounds");
    }

    @Test
    void auditTrailPublishesScalingEvents() {
        registerWorker("w1");
        registerWorker("w2");

        // Trigger manual scale-up
        autoScaler.triggerManual(ScalingAction.SCALE_UP, 1);

        // Consume from system.scaling topic
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = scalingConsumer.poll(Duration.ofMillis(500));
            List<ScalingEvent> events = new ArrayList<>();
            records.forEach(r -> {
                try {
                    CloudBalancerEvent event = JsonUtil.mapper().readValue(r.value(), CloudBalancerEvent.class);
                    if (event instanceof ScalingEvent se) {
                        events.add(se);
                    }
                } catch (Exception ignored) {}
            });
            assertThat(events).isNotEmpty();
            var lastEvent = events.getLast();
            assertThat(lastEvent.action()).isEqualTo(ScalingAction.SCALE_UP);
            assertThat(lastEvent.triggerType()).isEqualTo(ScalingTriggerType.MANUAL);
            assertThat(lastEvent.workersAdded()).isNotEmpty();
        });
    }

    @Test
    void drainingWorkerExcludedFromScheduling() {
        registerWorker("w1");
        registerWorker("w2");

        // Drain w2
        workerRegistry.drainWorker("w2");

        var available = workerRegistry.getAvailableWorkers();
        assertThat(available).hasSize(1);
        assertThat(available.get(0).getId()).isEqualTo("w1");
    }

    private void registerWorker(String id) {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of());
        workerRegistry.registerWorker(id, WorkerHealthState.HEALTHY, caps);
    }
}
