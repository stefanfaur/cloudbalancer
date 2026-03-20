package com.cloudbalancer.metrics.kafka;

import com.cloudbalancer.common.event.CloudBalancerEvent;
import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.metrics.persistence.TaskMetricsRecord;
import com.cloudbalancer.metrics.persistence.TaskMetricsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TaskEventsListener {

    private static final Logger log = LoggerFactory.getLogger(TaskEventsListener.class);

    private final TaskMetricsRepository repository;

    public TaskEventsListener(TaskMetricsRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "tasks.events", groupId = "metrics-aggregator-group")
    public void onTaskEvent(String message) {
        try {
            CloudBalancerEvent event = JsonUtil.mapper().readValue(message, CloudBalancerEvent.class);

            if (event instanceof TaskCompletedEvent completed) {
                var record = repository.findById(completed.taskId())
                        .orElse(new TaskMetricsRecord());
                record.setTaskId(completed.taskId());
                record.setCompletedAt(completed.timestamp());
                repository.save(record);

                log.debug("Stored task completion metrics for taskId={}", completed.taskId());
            }
            // Ignore other event types silently
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize task event: {}", e.getMessage(), e);
        }
    }
}
