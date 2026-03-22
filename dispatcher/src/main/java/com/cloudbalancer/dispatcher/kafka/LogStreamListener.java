package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.executor.LogMessage;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.websocket.LogStreamWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogStreamListener {

    private static final Logger log = LoggerFactory.getLogger(LogStreamListener.class);

    private final LogStreamWebSocketHandler webSocketHandler;

    public LogStreamListener(LogStreamWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @KafkaListener(topics = "tasks.logs", groupId = "log-stream-consumer")
    public void onLogMessage(String message) {
        try {
            LogMessage logMsg = JsonUtil.mapper().readValue(message, LogMessage.class);
            String json = JsonUtil.mapper().writeValueAsString(logMsg);
            webSocketHandler.broadcast(logMsg.taskId(), json);
        } catch (Exception e) {
            log.error("Failed to process log message", e);
        }
    }
}
