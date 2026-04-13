# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/websocket/WebSocketConfig.java

## Overview

`WebSocketConfig` is a Spring `@Configuration` class that enables and configures WebSocket support for the `dispatcher` service. It serves as the central registry for WebSocket handlers, mapping specific URL endpoints to their respective handler implementations and applying security interceptors.

## Public API

### `WebSocketConfig`
The class implements `WebSocketConfigurer` to provide custom configuration for WebSocket request handling.

#### Constructor
```java
public WebSocketConfig(LogStreamWebSocketHandler logStreamHandler,
                       DashboardWebSocketHandler dashboardWebSocketHandler,
                       JwtHandshakeInterceptor jwtHandshakeInterceptor)
```
Initializes the configuration with the required handlers and the JWT handshake interceptor for authentication.

#### `registerWebSocketHandlers(WebSocketHandlerRegistry registry)`
Registers the WebSocket handlers to the Spring application context:
*   **Log Stream Handler**: Mapped to `/api/tasks/*/logs/stream`.
*   **Dashboard Handler**: Mapped to `/api/ws/events`.
*   **Interceptors**: Applies `JwtHandshakeInterceptor` to both endpoints to ensure secure connections.
*   **CORS**: Sets allowed origins to `*` for both endpoints.

## Dependencies

*   `org.springframework.context.annotation.Configuration`
*   `org.springframework.web.socket.config.annotation.EnableWebSocket`
*   `org.springframework.web.socket.config.annotation.WebSocketConfigurer`
*   `org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry`
*   `LogStreamWebSocketHandler` (Injected)
*   `DashboardWebSocketHandler` (Injected)
*   `JwtHandshakeInterceptor` (Injected)

## Usage Notes

*   **Authentication**: All WebSocket connections established through this configuration are protected by the `JwtHandshakeInterceptor`. Clients must provide a valid JWT during the handshake process.
*   **Path Patterns**: The log stream endpoint supports path variables (e.g., `/api/tasks/{taskId}/logs/stream`). Ensure that the `LogStreamWebSocketHandler` is implemented to handle these dynamic path segments.
*   **CORS Policy**: Currently, `setAllowedOrigins("*")` is configured for both endpoints. In production environments, it is recommended to restrict these origins to specific trusted domains to enhance security.
*   **Registration**: This class is automatically detected by Spring's component scanning due to the `@Configuration` annotation. Ensure that the required handler beans are available in the application context.