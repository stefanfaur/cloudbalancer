package com.cloudbalancer.metrics.kafka;

import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRecord;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatMetricsListener {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMetricsListener.class);

    private final WorkerHeartbeatRepository repository;

    public HeartbeatMetricsListener(WorkerHeartbeatRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "workers.heartbeat", groupId = "metrics-aggregator-group")
    public void onHeartbeat(String message) {
        try {
            WorkerHeartbeatEvent event = JsonUtil.mapper().readValue(message, WorkerHeartbeatEvent.class);

            var record = new WorkerHeartbeatRecord();
            record.setWorkerId(event.workerId());
            record.setHealthState(event.healthState().name());
            record.setTimestamp(event.timestamp());
            repository.save(record);

            log.debug("Stored heartbeat for worker={} state={}", event.workerId(), event.healthState());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize WorkerHeartbeatEvent: {}", e.getMessage(), e);
        }
    }
}
