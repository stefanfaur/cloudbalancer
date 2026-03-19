package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.testutil.KafkaTestContainer;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaEventIntegrationTest {

    private static final String TOPIC = "test.events";
    private static final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void produceAndConsumeTaskSubmittedEvent() throws Exception {
        String bootstrapServers = KafkaTestContainer.getBootstrapServers();

        // Create event
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        UUID taskId = UUID.randomUUID();
        var event = new TaskSubmittedEvent(UUID.randomUUID().toString(), Instant.now(), taskId, descriptor);

        // Produce
        try (var producer = createProducer(bootstrapServers)) {
            String json = mapper.writeValueAsString(event);
            producer.send(new ProducerRecord<>(TOPIC, taskId.toString(), json)).get();
        }

        // Consume
        try (var consumer = createConsumer(bootstrapServers)) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(1);

            String json = records.iterator().next().value();
            CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);

            assertThat(deserialized).isInstanceOf(TaskSubmittedEvent.class);
            TaskSubmittedEvent received = (TaskSubmittedEvent) deserialized;
            assertThat(received.taskId()).isEqualTo(taskId);
            assertThat(received.descriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
            assertThat(received.eventType()).isEqualTo("TASK_SUBMITTED");
        }
    }

    @Test
    void produceAndConsumeWorkerRegisteredEvent() throws Exception {
        String bootstrapServers = KafkaTestContainer.getBootstrapServers();
        String topic = "test.worker.events";

        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of("region-us")
        );
        var event = new WorkerRegisteredEvent(
            UUID.randomUUID().toString(), Instant.now(), "worker-1", capabilities
        );

        try (var producer = createProducer(bootstrapServers)) {
            String json = mapper.writeValueAsString(event);
            producer.send(new ProducerRecord<>(topic, "worker-1", json)).get();
        }

        try (var consumer = createConsumer(bootstrapServers)) {
            consumer.subscribe(List.of(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(1);

            String json = records.iterator().next().value();
            CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);

            assertThat(deserialized).isInstanceOf(WorkerRegisteredEvent.class);
            WorkerRegisteredEvent received = (WorkerRegisteredEvent) deserialized;
            assertThat(received.workerId()).isEqualTo("worker-1");
            assertThat(received.capabilities().supportedExecutors()).contains(ExecutorType.SIMULATED);
        }
    }

    private KafkaProducer<String, String> createProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, String> createConsumer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
