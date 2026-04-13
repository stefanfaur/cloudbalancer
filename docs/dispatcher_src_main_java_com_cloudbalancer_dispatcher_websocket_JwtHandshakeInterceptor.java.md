# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/websocket/JwtHandshakeInterceptor.java

## Overview

`JwtHandshakeInterceptor` is a Spring `@Component` that implements the `HandshakeInterceptor` interface to provide security validation for WebSocket connections. It intercepts the initial HTTP handshake request to ensure that the client provides a valid JSON Web Token (JWT) before the WebSocket connection is established.

## Public API

### `JwtHandshakeInterceptor(JwtService jwtService)`
Constructs the interceptor with a required `JwtService` dependency used for token validation and claims extraction.

### `beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes)`
Intercepts the WebSocket handshake request. 
- **Logic**: Extracts the `token` query parameter from the request URI.
- **Validation**: If the token is missing, blank, or invalid (via `jwtService.isTokenValid`), it sets the HTTP response status to `401 Unauthorized` and aborts the handshake.
- **Success**: If valid, it extracts the username from the token and stores it in the `attributes` map, which is then accessible within the WebSocket session.

### `afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception)`
A no-op implementation required by the `HandshakeInterceptor` interface, executed after the handshake is completed.

## Dependencies

- `com.cloudbalancer.dispatcher.security.JwtService`: Used for validating JWT tokens and extracting user identity information.
- `org.springframework.web.socket.server.HandshakeInterceptor`: The base interface for intercepting WebSocket handshake requests.
- `org.springframework.http.server.ServerHttpRequest` / `ServerHttpResponse`: Spring abstractions for handling the underlying HTTP handshake transport.

## Usage Notes

- **Token Transmission**: This interceptor expects the JWT to be passed as a query parameter named `token` in the WebSocket handshake URL (e.g., `ws://example.com/socket?token=eyJhbG...`).
- **Session Attributes**: Upon successful authentication, the `username` extracted from the JWT is injected into the `attributes` map. This map is persisted in the `WebSocketSession`, allowing downstream handlers to retrieve the authenticated user's identity using `session.getAttributes().get("username")`.
- **Error Handling**: Any exception encountered during the token validation process results in an `HttpStatus.UNAUTHORIZED` response, preventing the WebSocket connection from upgrading.