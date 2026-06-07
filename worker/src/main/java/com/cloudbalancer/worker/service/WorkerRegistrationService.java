package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.config.WorkerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WorkerRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(WorkerRegistrationService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkerProperties properties;
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public WorkerRegistrationService(KafkaTemplate<String, String> kafkaTemplate, WorkerProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Idempotent registration, invoked when the tasks.assigned consumer gets
     * its partitions. Registering earlier (e.g. @PostConstruct) races the
     * dispatcher: it may assign tasks before the consumer's position is
     * fixed, and with auto-offset-reset=latest those assignments are lost.
     */
    public void registerOnce() {
        if (registered.compareAndSet(false, true)) {
            register();
        }
    }

    public void register() {
        var capabilities = new WorkerCapabilities(
            properties.getSupportedExecutors(),
            new ResourceProfile(
                properties.getCpuCores(), properties.getMemoryMb(), properties.getDiskMb(),
                false, 0, true
            ),
            properties.getTags()
        );
        var event = new WorkerRegisteredEvent(
            UUID.randomUUID().toString(), Instant.now(), properties.getId(), capabilities
        );

        try {
            String json = JsonUtil.mapper().writeValueAsString(event);
            kafkaTemplate.send("workers.registration", properties.getId(), json);
            log.info("Worker {} registered with capabilities: {}", properties.getId(), capabilities);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize registration event", e);
        }
    }
}
