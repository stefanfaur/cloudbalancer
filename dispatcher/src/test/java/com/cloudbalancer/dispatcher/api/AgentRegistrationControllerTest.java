package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.dispatcher.registration.AgentTokenService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AgentRegistrationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AgentTokenService tokenService;

    @Test
    void register_withValidToken_returnsKafkaCredentials() throws Exception {
        var result = tokenService.create("reg-test", "admin");

        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("agent-1", result.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kafkaBootstrap").isNotEmpty())
            .andExpect(jsonPath("$.kafkaUsername").isNotEmpty())
            .andExpect(jsonPath("$.kafkaPassword").isNotEmpty());
    }

    @Test
    void register_withInvalidToken_returns401() throws Exception {
        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("agent-bad", "cb_at_bogus")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withRevokedToken_returns401() throws Exception {
        var result = tokenService.create("revoked-reg", "admin");
        tokenService.revoke(result.id());

        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("agent-revoked", result.token())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withDuplicateAgentId_sameToken_succeeds() throws Exception {
        var result = tokenService.create("dup-test", "admin");

        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("agent-dup", result.token())))
            .andExpect(status().isOk());

        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("agent-dup", result.token())))
            .andExpect(status().isOk());
    }

    private String registrationBody(String agentId, String token) {
        return "{\"agentId\": \"%s\", \"token\": \"%s\", \"cpuCores\": 4, \"memoryMb\": 8192}"
            .formatted(agentId, token);
    }
}
