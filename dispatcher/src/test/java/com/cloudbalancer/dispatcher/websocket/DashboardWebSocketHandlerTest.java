package com.cloudbalancer.dispatcher.websocket;

import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardWebSocketHandlerTest {

    private DashboardWebSocketHandler handler;
    private WorkerRegistryService workerRegistryService;
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        workerRegistryService = mock(WorkerRegistryService.class);
        taskRepository = mock(TaskRepository.class);
        when(workerRegistryService.getAllWorkers()).thenReturn(Collections.emptyList());
        when(taskRepository.countByState(any())).thenReturn(0L);
        handler = new DashboardWebSocketHandler(workerRegistryService, taskRepository);
    }

    @Test
    void broadcastSendsMessageToConnectedSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.broadcast("TASK_UPDATE", Map.of("id", "test-123"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        // initial snapshot + broadcast = 2 messages
        verify(session, times(2)).sendMessage(captor.capture());
        String json = captor.getAllValues().get(1).getPayload();
        JsonNode node = JsonUtil.mapper().readTree(json);
        assertThat(node.get("type").asText()).isEqualTo("TASK_UPDATE");
        assertThat(node.get("payload").get("id").asText()).isEqualTo("test-123");
    }

    @Test
    void closedSessionIsNotSentMessages() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        when(session.isOpen()).thenReturn(false);
        handler.broadcast("TASK_UPDATE", Map.of("id", "test"));

        // Only initial snapshot should have been sent
        verify(session, times(1)).sendMessage(any());
    }

    @Test
    void ioExceptionOnOneSessionDoesNotPreventDeliveryToOthers() throws Exception {
        WebSocketSession bad = mock(WebSocketSession.class);
        when(bad.isOpen()).thenReturn(true);
        when(bad.getId()).thenReturn("bad");
        doThrow(new IOException("broken")).when(bad).sendMessage(any());

        WebSocketSession good = mock(WebSocketSession.class);
        when(good.isOpen()).thenReturn(true);
        when(good.getId()).thenReturn("good");

        // Need to suppress initial snapshot errors too
        handler.afterConnectionEstablished(bad);
        handler.afterConnectionEstablished(good);

        handler.broadcast("TEST", "payload");

        // good should have received initial snapshot + broadcast
        verify(good, times(2)).sendMessage(any());
    }

    @Test
    void initialSnapshotSentOnConnection() throws Exception {
        when(taskRepository.countByState(TaskState.RUNNING)).thenReturn(5L);
        when(taskRepository.countByState(TaskState.QUEUED)).thenReturn(3L);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        JsonNode node = JsonUtil.mapper().readTree(captor.getValue().getPayload());
        assertThat(node.get("type").asText()).isEqualTo("INITIAL_SNAPSHOT");
        assertThat(node.get("payload").get("queuedTaskCount").asLong()).isEqualTo(3);
    }

    @Test
    void sessionRemovedAfterClose() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        assertThat(handler.getSessionCount()).isEqualTo(1);

        handler.afterConnectionClosed(session, null);
        assertThat(handler.getSessionCount()).isEqualTo(0);
    }
}
