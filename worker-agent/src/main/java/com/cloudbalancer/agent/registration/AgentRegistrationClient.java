package com.cloudbalancer.agent.registration;

import com.cloudbalancer.agent.config.AgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AgentRegistrationClient {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistrationClient.class);
    private static final int MAX_RETRIES = 10;
    private static final long MAX_BACKOFF_MS = 60_000;

    private final AgentProperties props;
    private final ObjectMapper objectMapper;
    private volatile RegistrationResult cachedResult;

    public AgentRegistrationClient(AgentProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public RegistrationResult register() {
        String token = props.getRegistrationToken();
        if (token == null || token.isBlank()) {
            log.info("No registration token configured — running in local mode");
            return null;
        }

        String url = props.getDispatcherUrl() + "/api/agents/register";
        log.info("Registering agent {} with dispatcher at {}", props.getId(), props.getDispatcherUrl());

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            String body = objectMapper.writeValueAsString(new RegistrationRequest(
                props.getId(), token,
                (int) props.getTotalCpuCores(),
                (int) props.getTotalMemoryMb()));

            long backoffMs = 1000;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JsonNode json = objectMapper.readTree(response.body());
                        cachedResult = new RegistrationResult(
                            json.get("kafkaBootstrap").asText(),
                            json.get("kafkaUsername").asText(),
                            json.get("kafkaPassword").asText()
                        );
                        log.info("Registration successful — Kafka bootstrap: {}", cachedResult.kafkaBootstrap());
                        return cachedResult;
                    } else if (response.statusCode() == 401) {
                        log.error("Registration failed: invalid or revoked token. Exiting.");
                        System.exit(1);
                    } else {
                        log.warn("Registration attempt {}/{} returned status {}, retrying in {}ms",
                            attempt, MAX_RETRIES, response.statusCode(), backoffMs);
                    }
                } catch (Exception e) {
                    log.warn("Registration attempt {}/{} failed: {}, retrying in {}ms",
                        attempt, MAX_RETRIES, e.getMessage(), backoffMs);
                }

                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                }
            }
        } catch (Exception e) {
            log.error("Registration failed after all retries", e);
        }

        log.error("Registration failed after {} attempts. Exiting.", MAX_RETRIES);
        System.exit(1);
        return null; // unreachable
    }

    public RegistrationResult getCachedResult() {
        return cachedResult;
    }

    public record RegistrationResult(String kafkaBootstrap, String kafkaUsername, String kafkaPassword) {}

    private record RegistrationRequest(String agentId, String token, int cpuCores, int memoryMb) {}
}
