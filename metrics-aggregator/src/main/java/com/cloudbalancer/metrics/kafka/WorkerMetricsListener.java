package com.cloudbalancer.metrics.kafka;

import com.cloudbalancer.common.event.WorkerMetricsEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRecord;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetricsListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerMetricsListener.class);

    private final WorkerMetricsRepository repository;

    public WorkerMetricsListener(WorkerMetricsRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "workers.metrics", groupId = "metrics-aggregator-group")
    public void onWorkerMetrics(String message) {
        try {
            WorkerMetricsEvent event = JsonUtil.mapper().readValue(message, WorkerMetricsEvent.class);

            var record = new WorkerMetricsRecord();
            record.setWorkerId(event.workerId());
            record.setCpuUsagePercent(event.metrics().cpuUsagePercent());
            record.setHeapUsedMB(event.metrics().heapUsedMB());
            record.setHeapMaxMB(event.metrics().heapMaxMB());
            record.setThreadCount(event.metrics().threadCount());
            record.setActiveTaskCount(event.metrics().activeTaskCount());
            record.setCompletedTaskCount(event.metrics().completedTaskCount());
            record.setFailedTaskCount(event.metrics().failedTaskCount());
            record.setAvgExecutionDurationMs(event.metrics().averageExecutionDurationMs());
            record.setReportedAt(event.metrics().reportedAt());
            repository.save(record);

            log.debug("Stored worker metrics for worker={}", event.workerId());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize WorkerMetricsEvent: {}", e.getMessage(), e);
        }
    }
}
