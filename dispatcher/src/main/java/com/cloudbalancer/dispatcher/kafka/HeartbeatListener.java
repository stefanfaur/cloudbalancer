package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.WorkerHeartbeatEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.HeartbeatTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatListener {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatListener.class);
    private final HeartbeatTracker heartbeatTracker;

    public HeartbeatListener(HeartbeatTracker heartbeatTracker) {
        this.heartbeatTracker = heartbeatTracker;
    }

    @KafkaListener(topics = "workers.heartbeat", groupId = "dispatcher")
    public void onHeartbeat(String message) {
        try {
            WorkerHeartbeatEvent event = JsonUtil.mapper().readValue(message, WorkerHeartbeatEvent.class);
            heartbeatTracker.onHeartbeat(event.workerId(), event.timestamp());
            log.debug("Processed heartbeat from worker {}", event.workerId());
        } catch (Exception e) {
            log.error("Failed to process heartbeat", e);
        }
    }
}
