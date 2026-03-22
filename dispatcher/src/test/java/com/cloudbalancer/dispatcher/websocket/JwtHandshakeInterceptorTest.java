package com.cloudbalancer.dispatcher.websocket;

import com.cloudbalancer.dispatcher.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtHandshakeInterceptorTest {

    private JwtService jwtService;
    private JwtHandshakeInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        interceptor = new JwtHandshakeInterceptor(jwtService);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    @Test
    void validTokenAllowsHandshake() throws Exception {
        when(request.getURI()).thenReturn(new URI("/api/tasks/123/logs/stream?token=valid-jwt"));
        when(jwtService.isTokenValid("valid-jwt")).thenReturn(true);
        when(jwtService.extractUsername("valid-jwt")).thenReturn("testuser");

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry("username", "testuser");
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void missingTokenRejectsHandshake() throws Exception {
        when(request.getURI()).thenReturn(new URI("/api/tasks/123/logs/stream"));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void blankTokenRejectsHandshake() throws Exception {
        when(request.getURI()).thenReturn(new URI("/api/tasks/123/logs/stream?token="));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidTokenRejectsHandshake() throws Exception {
        when(request.getURI()).thenReturn(new URI("/api/tasks/123/logs/stream?token=bad-jwt"));
        when(jwtService.isTokenValid("bad-jwt")).thenReturn(false);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertThat(attributes).doesNotContainKey("username");
    }

    @Test
    void exceptionDuringValidationRejectsHandshake() throws Exception {
        when(request.getURI()).thenReturn(new URI("/api/tasks/123/logs/stream?token=error-jwt"));
        when(jwtService.isTokenValid("error-jwt")).thenThrow(new RuntimeException("parse failure"));

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
