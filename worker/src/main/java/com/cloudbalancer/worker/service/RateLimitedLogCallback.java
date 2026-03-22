package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.executor.LogCallback;
import com.cloudbalancer.common.executor.LogMessage;
import com.cloudbalancer.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class RateLimitedLogCallback implements LogCallback {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedLogCallback.class);

    private final UUID taskId;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long minIntervalMs;
    private long lastSendTime = 0;
    private final List<LogMessage> buffer = new ArrayList<>();

    RateLimitedLogCallback(UUID taskId, KafkaTemplate<String, String> kafkaTemplate, long minIntervalMs) {
        this.taskId = taskId;
        this.kafkaTemplate = kafkaTemplate;
        this.minIntervalMs = minIntervalMs;
    }

    @Override
    public synchronized void onLogLine(String line, boolean isStderr, Instant timestamp) {
        buffer.add(new LogMessage(taskId, line, isStderr, timestamp));
        long now = System.currentTimeMillis();
        if (now - lastSendTime >= minIntervalMs) {
            flush();
        }
    }

    public synchronized void flush() {
        if (buffer.isEmpty()) return;
        for (LogMessage msg : buffer) {
            try {
                String json = JsonUtil.mapper().writeValueAsString(msg);
                kafkaTemplate.send("tasks.logs", taskId.toString(), json);
            } catch (Exception e) {
                log.debug("Failed to publish log line for task {}: {}", taskId, e.getMessage());
            }
        }
        buffer.clear();
        lastSendTime = System.currentTimeMillis();
    }
}
