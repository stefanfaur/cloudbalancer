package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.executor.*;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simulates a worker for integration tests.
 * Registers via Kafka, consumes task assignments, executes, publishes results.
 */
public class TestWorkerSimulator implements AutoCloseable {

    private final String workerId;
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObjectMapper mapper = JsonUtil.mapper();
    private volatile boolean running = true;
    private final Set<ExecutorType> supportedExecutorTypes;
    private final Map<ExecutorType, TaskExecutor> executors;

    /** Original constructor — backward compatible, supports only SIMULATED. */
    public TestWorkerSimulator(String workerId, String bootstrapServers) {
        this(workerId, bootstrapServers, Set.of(ExecutorType.SIMULATED), Map.of(ExecutorType.SIMULATED, new SimulatedExecutor()));
    }

    /** Extended constructor — supports configurable executor types. */
    public TestWorkerSimulator(String workerId, String bootstrapServers,
                               Set<ExecutorType> supportedExecutorTypes,
                               Map<ExecutorType, TaskExecutor> executors) {
        this.workerId = workerId;
        this.supportedExecutorTypes = supportedExecutorTypes;
        this.executors = executors;
        this.producer = createProducer(bootstrapServers);
        this.consumer = createConsumer(bootstrapServers);
    }

    public void start() throws Exception {
        // Register
        var caps = new WorkerCapabilities(
            supportedExecutorTypes,
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of()
        );
        var event = new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(), workerId, caps);
        producer.send(new ProducerRecord<>("workers.registration", workerId, mapper.writeValueAsString(event))).get();

        // Consume assignments — subscribe and wait for partition assignment
        consumer.subscribe(List.of("tasks.assigned"));
        // Trigger initial partition assignment before entering the processing loop
        int attempts = 0;
        while (consumer.assignment().isEmpty() && attempts < 20) {
            consumer.poll(Duration.ofMillis(500));
            attempts++;
        }
        executor.submit(() -> {
            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    for (var record : records) {
                        processAssignment(record.value());
                    }
                } catch (Exception e) {
                    if (running) e.printStackTrace();
                }
            }
        });
    }

    private void processAssignment(String value) {
        try {
            TaskAssignment assignment = mapper.readValue(value, TaskAssignment.class);
            ExecutorType type = assignment.descriptor().executorType();
            TaskExecutor taskExec = executors.get(type);
            if (taskExec == null) {
                taskExec = new SimulatedExecutor(); // fallback
            }
            var allocation = new ResourceAllocation(1, 512, 256);
            var context = new TaskContext(assignment.taskId(), Path.of(System.getProperty("java.io.tmpdir")));

            int timeout = assignment.descriptor().executionPolicy() != null
                ? assignment.descriptor().executionPolicy().timeoutSeconds() : 300;
            ExecutionResult result;
            ExecutorService taskRunner = Executors.newSingleThreadExecutor();
            final TaskExecutor finalExec = taskExec;
            try {
                Future<ExecutionResult> future = taskRunner.submit(() ->
                    finalExec.execute(assignment.descriptor().executionSpec(), allocation, context)
                );
                result = future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                result = new ExecutionResult(1, "", "Timed out", timeout * 1000L, true);
            } finally {
                taskRunner.shutdownNow();
            }

            var taskResult = new TaskResult(
                assignment.taskId(), workerId, result.exitCode(),
                result.stdout(), result.stderr(), result.durationMs(),
                result.timedOut(), Instant.now(), assignment.executionId()
            );
            producer.send(new ProducerRecord<>("tasks.results", assignment.taskId().toString(),
                mapper.writeValueAsString(taskResult))).get();
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }

    @Override
    public void close() {
        running = false;
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try { consumer.close(Duration.ofSeconds(5)); } catch (Exception ignored) {}
        try { producer.close(Duration.ofSeconds(5)); } catch (Exception ignored) {}
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, workerId + "-" + UUID.randomUUID().toString().substring(0, 8));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
