package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:0");
    }

    @LocalServerPort private int port;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.create("http://localhost:" + port);
    }

    @Test
    void fullAuthFlow_loginAccessRefreshAccess() throws Exception {
        // Seed admin is created by DataSeeder

        // 1. Login
        String loginResponse = restClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("username", "admin", "password", "admin"))
            .retrieve()
            .body(String.class);

        var loginJson = JsonUtil.mapper().readTree(loginResponse);
        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 2. Access secured endpoint with token
        String tasksResponse = restClient.get()
            .uri("/api/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(String.class);
        assertThat(tasksResponse).isNotNull();

        // 3. Refresh
        String refreshResponse = restClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("refreshToken", refreshToken))
            .retrieve()
            .body(String.class);

        var refreshJson = JsonUtil.mapper().readTree(refreshResponse);
        String newAccessToken = refreshJson.get("accessToken").asText();
        assertThat(newAccessToken).isNotBlank();

        // 4. Access with new token
        String tasks2 = restClient.get()
            .uri("/api/tasks")
            .header("Authorization", "Bearer " + newAccessToken)
            .retrieve()
            .body(String.class);
        assertThat(tasks2).isNotNull();
    }

    @Test
    void unauthenticatedRequestReturns401() {
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/tasks")
                .retrieve()
                .body(String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .hasMessageContaining("401");
    }
}
