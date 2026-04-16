package com.cloudbalancer.agent.kafka;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.service.ContainerManager;
import com.cloudbalancer.common.agent.AgentCommand;
import com.cloudbalancer.common.agent.AgentEvent;
import com.cloudbalancer.common.model.DrainCommand;
import com.cloudbalancer.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AgentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(AgentCommandListener.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ContainerManager containerManager;
    private final AgentProperties props;
    private final ScheduledExecutorService drainScheduler = Executors.newSingleThreadScheduledExecutor(
        r -> { Thread t = new Thread(r, "agent-drain-scheduler"); t.setDaemon(true); return t; }
    );

    public AgentCommandListener(KafkaTemplate<String, String> kafkaTemplate,
                                ContainerManager containerManager,
                                AgentProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.containerManager = containerManager;
        this.props = props;
    }

    @KafkaListener(topics = "agents.commands", groupId = "agent-${cloudbalancer.agent.id:agent-1}")
    public void onCommand(String message) {
        try {
            AgentCommand cmd = JsonUtil.mapper().readValue(message, AgentCommand.class);

            if (!props.getId().equals(cmd.agentId())) {
                return;
            }

            switch (cmd) {
                case AgentCommand.StartWorkerCommand start -> handleStart(start);
                case AgentCommand.StopWorkerCommand stop -> handleStop(stop);
            }
        } catch (Exception e) {
            log.error("Failed to process agent command", e);
        }
    }

    private void handleStart(AgentCommand.StartWorkerCommand cmd) {
        // Publish ContainerCreatingEvent before Docker operation
        publishEvent(new AgentEvent.ContainerCreatingEvent(
            props.getId(), cmd.workerId(), Instant.now()));

        try {
            String containerId = containerManager.startWorker(
                cmd.workerId(), cmd.cpuCores(), cmd.memoryMB(),
                "WORKER_ID=" + cmd.workerId());

            publishEvent(new AgentEvent.WorkerStartedEvent(
                props.getId(), cmd.workerId(), containerId, Instant.now()));

            log.info("Started worker {} (container: {})", cmd.workerId(), containerId);
        } catch (Exception e) {
            log.error("Failed to start worker {}", cmd.workerId(), e);
            publishEvent(new AgentEvent.WorkerStartFailedEvent(
                props.getId(), cmd.workerId(), e.getMessage(), Instant.now()));
        }
    }

    private void handleStop(AgentCommand.StopWorkerCommand cmd) {
        if (cmd.drain()) {
            try {
                var drainCmd = new DrainCommand(cmd.workerId(), cmd.drainTimeSeconds(), Instant.now());
                String json = JsonUtil.mapper().writeValueAsString(drainCmd);
                kafkaTemplate.send("workers.commands", cmd.workerId(), json);
                log.info("Published drain command for worker {}, scheduling stop in {}s",
                    cmd.workerId(), cmd.drainTimeSeconds());
            } catch (Exception e) {
                log.error("Failed to publish drain command for worker {}", cmd.workerId(), e);
            }

            drainScheduler.schedule(() -> doStop(cmd.workerId()), cmd.drainTimeSeconds(), TimeUnit.SECONDS);
        } else {
            doStop(cmd.workerId());
        }
    }

    private void doStop(String workerId) {
        try {
            containerManager.stopWorker(workerId);
            publishEvent(new AgentEvent.WorkerStoppedEvent(props.getId(), workerId, Instant.now()));
            log.info("Stopped worker {}", workerId);
        } catch (Exception e) {
            log.error("Failed to stop worker {}: {}", workerId, e.getMessage());
            publishEvent(new AgentEvent.WorkerStopFailedEvent(
                props.getId(), workerId, e.getMessage(), Instant.now()));
        }
    }

    private void publishEvent(AgentEvent event) {
        try {
            String json = JsonUtil.mapper().writeValueAsString(event);
            kafkaTemplate.send("agents.events", props.getId(), json);
        } catch (Exception e) {
            log.error("Failed to publish agent event", e);
        }
    }
}
