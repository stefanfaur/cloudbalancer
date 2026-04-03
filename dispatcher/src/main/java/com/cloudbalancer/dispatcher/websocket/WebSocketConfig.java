package com.cloudbalancer.dispatcher.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogStreamWebSocketHandler logStreamHandler;
    private final DashboardWebSocketHandler dashboardWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(LogStreamWebSocketHandler logStreamHandler,
                           DashboardWebSocketHandler dashboardWebSocketHandler,
                           JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.logStreamHandler = logStreamHandler;
        this.dashboardWebSocketHandler = dashboardWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logStreamHandler, "/api/tasks/*/logs/stream")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
        registry.addHandler(dashboardWebSocketHandler, "/api/ws/events")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
