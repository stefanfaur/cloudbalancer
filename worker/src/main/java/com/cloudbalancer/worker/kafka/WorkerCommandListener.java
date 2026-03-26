package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.DrainCommand;
import com.cloudbalancer.common.model.WorkerCommand;
import com.cloudbalancer.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerCommandListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerCommandListener.class);
    private final String workerId;
    private final AtomicBoolean draining;

    public WorkerCommandListener(
            @Value("${cloudbalancer.worker.id:worker-1}") String workerId,
            AtomicBoolean draining) {
        this.workerId = workerId;
        this.draining = draining;
    }

    @KafkaListener(topics = "workers.commands", groupId = "${cloudbalancer.worker.id:worker-1}")
    public void onCommand(String message) {
        try {
            WorkerCommand command = JsonUtil.mapper().readValue(message, WorkerCommand.class);
            if (!command.workerId().equals(workerId)) {
                return;
            }
            if (command instanceof DrainCommand drain) {
                log.info("Received drain command, drain time: {}s", drain.drainTimeSeconds());
                draining.set(true);
            }
        } catch (Exception e) {
            log.error("Failed to process worker command", e);
        }
    }
}
