package com.cloudbalancer.agent.kafka;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.service.ContainerManager;
import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.agent.AgentRegisteredEvent;
import com.cloudbalancer.common.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AgentHeartbeatPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgentHeartbeatPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AgentProperties props;
    private final ContainerManager containerManager;

    public AgentHeartbeatPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   AgentProperties props,
                                   ContainerManager containerManager) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.containerManager = containerManager;
    }

    @PostConstruct
    public void publishRegistration() {
        try {
            var event = new AgentRegisteredEvent(
                props.getId(), props.getHostname(),
                props.getTotalCpuCores(), props.getTotalMemoryMb(),
                props.getSupportedExecutors(), Instant.now());

            String json = JsonUtil.mapper().writeValueAsString(event);
            kafkaTemplate.send("agents.registration", props.getId(), json);
            log.info("Published agent registration: {}", props.getId());
        } catch (Exception e) {
            log.error("Failed to publish agent registration", e);
        }
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.agent.heartbeat-interval-ms:10000}")
    public void publishHeartbeat() {
        try {
            var activeWorkers = containerManager.getActiveWorkerIds();
            double usedCpu = activeWorkers.size();
            long usedMemory = activeWorkers.size() * 2048L;

            var heartbeat = new AgentHeartbeat(
                props.getId(), props.getHostname(),
                props.getTotalCpuCores(),
                Math.max(0, props.getTotalCpuCores() - usedCpu),
                props.getTotalMemoryMb(),
                Math.max(0, props.getTotalMemoryMb() - usedMemory),
                activeWorkers,
                props.getSupportedExecutors(),
                Instant.now());

            String json = JsonUtil.mapper().writeValueAsString(heartbeat);
            kafkaTemplate.send("agents.heartbeat", props.getId(), json);
        } catch (Exception e) {
            log.error("Failed to publish agent heartbeat", e);
        }
    }
}
