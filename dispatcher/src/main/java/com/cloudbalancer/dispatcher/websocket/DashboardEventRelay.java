package com.cloudbalancer.dispatcher.websocket;

import com.cloudbalancer.common.agent.AgentEvent;
import com.cloudbalancer.common.event.*;
import com.cloudbalancer.common.model.TaskEnvelope;
import com.cloudbalancer.common.model.TaskResult;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DashboardEventRelay {

    private static final Logger log = LoggerFactory.getLogger(DashboardEventRelay.class);

    private final DashboardWebSocketHandler handler;
    private final TaskService taskService;

    public DashboardEventRelay(DashboardWebSocketHandler handler, TaskService taskService) {
        this.handler = handler;
        this.taskService = taskService;
    }

    @KafkaListener(topics = "tasks.results", groupId = "dashboard-relay")
    public void onTaskResult(String message) {
        try {
            TaskResult result = JsonUtil.mapper().readValue(message, TaskResult.class);
            TaskEnvelope envelope = taskService.getTask(result.taskId());
            if (envelope != null) {
                handler.broadcast("TASK_UPDATE", envelope);
            }
        } catch (Exception e) {
            log.warn("Failed to process task result for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "tasks.events", groupId = "dashboard-relay")
    public void onTaskEvent(String message) {
        try {
            CloudBalancerEvent event = JsonUtil.mapper().readValue(message, CloudBalancerEvent.class);
            if (event instanceof TaskStateChangedEvent changed) {
                TaskEnvelope envelope = taskService.getTask(changed.taskId());
                if (envelope != null) {
                    handler.broadcast("TASK_UPDATE", envelope);
                }
            } else if (event instanceof TaskCompletedEvent completed) {
                TaskEnvelope envelope = taskService.getTask(completed.taskId());
                if (envelope != null) {
                    handler.broadcast("TASK_UPDATE", envelope);
                }
            } else if (event instanceof TaskAssignedEvent assigned) {
                TaskEnvelope envelope = taskService.getTask(assigned.taskId());
                if (envelope != null) {
                    handler.broadcast("TASK_UPDATE", envelope);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process task event for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "workers.heartbeat", groupId = "dashboard-relay")
    public void onWorkerHeartbeat(String message) {
        try {
            WorkerHeartbeatEvent event = JsonUtil.mapper().readValue(message, WorkerHeartbeatEvent.class);
            handler.broadcast("WORKER_STATE", Map.of(
                "workerId", event.workerId(),
                "state", event.healthState().name()
            ));
        } catch (Exception e) {
            log.warn("Failed to process heartbeat for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "workers.metrics", groupId = "dashboard-relay")
    public void onWorkerMetrics(String message) {
        try {
            WorkerMetricsEvent event = JsonUtil.mapper().readValue(message, WorkerMetricsEvent.class);
            handler.broadcast("WORKER_UPDATE", event.metrics());
        } catch (Exception e) {
            log.warn("Failed to process worker metrics for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "system.scaling", groupId = "dashboard-relay")
    public void onScalingEvent(String message) {
        try {
            ScalingEvent event = JsonUtil.mapper().readValue(message, ScalingEvent.class);
            handler.broadcast("SCALING_EVENT", event);
        } catch (Exception e) {
            log.warn("Failed to process scaling event for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "agents.events", groupId = "dashboard-relay")
    public void onAgentEvent(String message) {
        try {
            AgentEvent event = JsonUtil.mapper().readValue(message, AgentEvent.class);
            switch (event) {
                case AgentEvent.ContainerCreatingEvent creating ->
                    handler.broadcast("CONTAINER_STARTING", Map.of(
                        "agentId", creating.agentId(),
                        "workerId", creating.workerId(),
                        "timestamp", creating.timestamp().toString()));
                case AgentEvent.WorkerStartedEvent started ->
                    handler.broadcast("CONTAINER_STARTED", Map.of(
                        "agentId", started.agentId(),
                        "workerId", started.workerId(),
                        "containerId", started.containerId(),
                        "timestamp", started.timestamp().toString()));
                case AgentEvent.WorkerStartFailedEvent failed ->
                    handler.broadcast("CONTAINER_FAILED", Map.of(
                        "agentId", failed.agentId(),
                        "workerId", failed.workerId(),
                        "error", failed.reason(),
                        "timestamp", failed.timestamp().toString()));
                case AgentEvent.WorkerStoppedEvent stopped ->
                    handler.broadcast("WORKER_STOPPED", Map.of(
                        "agentId", stopped.agentId(),
                        "workerId", stopped.workerId(),
                        "timestamp", stopped.timestamp().toString()));
                case AgentEvent.WorkerStopFailedEvent stopFailed ->
                    handler.broadcast("WORKER_STOP_FAILED", Map.of(
                        "agentId", stopFailed.agentId(),
                        "workerId", stopFailed.workerId(),
                        "error", stopFailed.reason(),
                        "timestamp", stopFailed.timestamp().toString()));
            }
        } catch (Exception e) {
            log.warn("Failed to process agent event for dashboard: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "workers.registration", groupId = "dashboard-relay")
    public void onWorkerRegistered(String message) {
        try {
            WorkerRegisteredEvent event = JsonUtil.mapper().readValue(message, WorkerRegisteredEvent.class);
            handler.broadcast("WORKER_REGISTERED", Map.of(
                "workerId", event.workerId(),
                "timestamp", event.timestamp().toString()));
        } catch (Exception e) {
            log.warn("Failed to process worker registration for dashboard: {}", e.getMessage());
        }
    }
}
