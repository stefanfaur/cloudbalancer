package com.cloudbalancer.dispatcher.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LogStreamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogStreamWebSocketHandler.class);

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID taskId = extractTaskId(session);
        if (taskId != null) {
            subscriptions.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("WebSocket session opened for task {}", taskId);
        } else {
            log.warn("WebSocket connection with unparseable taskId, closing: {}", session.getUri());
            try { session.close(CloseStatus.BAD_DATA); } catch (IOException ignored) { }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID taskId = extractTaskId(session);
        if (taskId != null) {
            Set<WebSocketSession> sessions = subscriptions.get(taskId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    subscriptions.remove(taskId);
                }
            }
            log.debug("WebSocket session closed for task {} (status: {})", taskId, status);
        }
    }

    public void broadcast(UUID taskId, String logLine) {
        Set<WebSocketSession> sessions = subscriptions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(logLine);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Failed to send log message to session {} for task {}: {}",
                            session.getId(), taskId, e.getMessage());
                }
            }
        }
    }

    private UUID extractTaskId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        // URI pattern: /api/tasks/{taskId}/logs/stream
        // segments: ["", "api", "tasks", "{taskId}", "logs", "stream"]
        String path = uri.getPath();
        String[] segments = path.split("/");
        if (segments.length >= 4) {
            try {
                return UUID.fromString(segments[3]);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
