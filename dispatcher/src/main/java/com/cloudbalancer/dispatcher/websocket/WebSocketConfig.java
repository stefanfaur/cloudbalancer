package com.cloudbalancer.dispatcher.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogStreamWebSocketHandler logStreamHandler;

    public WebSocketConfig(LogStreamWebSocketHandler logStreamHandler) {
        this.logStreamHandler = logStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logStreamHandler, "/api/tasks/*/logs/stream")
                .setAllowedOrigins("*");
    }
}
