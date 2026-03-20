package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.model.WorkerInfo;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class WorkerRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerRegistrationListener.class);
    private final WorkerRegistryService workerRegistry;

    public WorkerRegistrationListener(WorkerRegistryService workerRegistry) {
        this.workerRegistry = workerRegistry;
    }

    @KafkaListener(topics = "workers.registration", groupId = "dispatcher")
    public void onWorkerRegistered(String message) {
        try {
            WorkerRegisteredEvent event = JsonUtil.mapper().readValue(message, WorkerRegisteredEvent.class);
            var workerInfo = new WorkerInfo(
                event.workerId(), WorkerHealthState.HEALTHY,
                event.capabilities(), null, Instant.now()
            );
            workerRegistry.registerWorker(workerInfo);
            log.info("Worker registered: {}", event.workerId());
        } catch (Exception e) {
            log.error("Failed to process worker registration", e);
        }
    }
}
