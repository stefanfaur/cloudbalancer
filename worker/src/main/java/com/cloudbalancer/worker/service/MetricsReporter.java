package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.config.WorkerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.UUID;

@Service
public class MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger(MetricsReporter.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkerProperties properties;
    private final TaskExecutionService taskExecutionService;

    public MetricsReporter(KafkaTemplate<String, String> kafkaTemplate,
                           WorkerProperties properties,
                           TaskExecutionService taskExecutionService) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.taskExecutionService = taskExecutionService;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.worker.metrics-interval-ms:5000}")
    public void publishMetrics() {
        WorkerMetrics metrics = collectMetrics();
        var event = new WorkerMetricsEvent(
            UUID.randomUUID().toString(), Instant.now(),
            properties.getId(), metrics
        );
        publish("workers.metrics", properties.getId(), event);
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.worker.heartbeat-interval-ms:10000}")
    public void publishHeartbeat() {
        var event = new WorkerHeartbeatEvent(
            UUID.randomUUID().toString(), Instant.now(),
            properties.getId(), WorkerHealthState.HEALTHY
        );
        publish("workers.heartbeat", properties.getId(), event);
    }

    private WorkerMetrics collectMetrics() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        double cpuUsage = os.getSystemLoadAverage() / os.getAvailableProcessors() * 100;
        if (cpuUsage < 0) cpuUsage = 0;
        long heapUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long heapMaxMB = runtime.maxMemory() / (1024 * 1024);
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        return new WorkerMetrics(
            cpuUsage, heapUsedMB, heapMaxMB, threadCount,
            taskExecutionService.getActiveTaskCount(),
            taskExecutionService.getCompletedTaskCount(),
            taskExecutionService.getFailedTaskCount(),
            taskExecutionService.getAverageExecutionDurationMs(),
            Instant.now()
        );
    }

    private void publish(String topic, String key, Object event) {
        try {
            String json = JsonUtil.mapper().writeValueAsString(event);
            kafkaTemplate.send(topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic {}", topic, e);
        }
    }
}
