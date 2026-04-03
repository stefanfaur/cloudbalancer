package com.cloudbalancer.dispatcher.websocket;

import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final WorkerRegistryService workerRegistryService;
    private final TaskRepository taskRepository;

    public DashboardWebSocketHandler(WorkerRegistryService workerRegistryService,
                                      TaskRepository taskRepository) {
        this.workerRegistryService = workerRegistryService;
        this.taskRepository = taskRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("Dashboard WebSocket session opened: {}", session.getId());
        sendInitialSnapshot(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("Dashboard WebSocket session closed: {} ({})", session.getId(), status);
    }

    public void broadcast(String type, Object payload) {
        if (sessions.isEmpty()) return;

        String json;
        try {
            json = JsonUtil.mapper().writeValueAsString(Map.of("type", type, "payload", payload));
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket message: {}", e.getMessage());
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    public int getSessionCount() {
        return sessions.size();
    }

    private void sendInitialSnapshot(WebSocketSession session) {
        try {
            var workers = workerRegistryService.getAllWorkers().stream()
                .map(w -> Map.of(
                    "workerId", w.getId(),
                    "healthState", w.getHealthState().name(),
                    "activeTaskCount", w.getActiveTaskCount()
                ))
                .toList();

            long activeTaskCount = taskRepository.countByState(TaskState.RUNNING)
                + taskRepository.countByState(TaskState.ASSIGNED)
                + taskRepository.countByState(TaskState.PROVISIONING);
            long queuedTaskCount = taskRepository.countByState(TaskState.QUEUED);

            var snapshot = Map.of(
                "type", "INITIAL_SNAPSHOT",
                "payload", Map.of(
                    "workers", workers,
                    "activeTaskCount", activeTaskCount,
                    "queuedTaskCount", queuedTaskCount
                )
            );

            String json = JsonUtil.mapper().writeValueAsString(snapshot);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send initial snapshot: {}", e.getMessage());
        }
    }
}
