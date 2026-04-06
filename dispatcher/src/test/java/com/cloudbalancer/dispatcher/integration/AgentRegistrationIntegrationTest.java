package com.cloudbalancer.dispatcher.integration;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AgentRegistrationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fullFlow_createToken_register_revoke_reject() throws Exception {
        String adminToken = jwtService.generateAccessToken("admin", Role.ADMIN);

        // 1. Create token via admin endpoint
        String createResponse = mvc.perform(post("/api/admin/agent-tokens")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\": \"integration-test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn().getResponse().getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        String agentToken = createJson.get("token").asText();
        String tokenId = createJson.get("id").asText();

        // 2. Register with valid token (no JWT needed)
        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\": \"integration-agent\", \"token\": \"%s\", \"cpuCores\": 4, \"memoryMb\": 8192}"
                    .formatted(agentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kafkaBootstrap").isNotEmpty())
            .andExpect(jsonPath("$.kafkaUsername").isNotEmpty())
            .andExpect(jsonPath("$.kafkaPassword").isNotEmpty());

        // 3. Revoke token
        mvc.perform(post("/api/admin/agent-tokens/" + tokenId + "/revoke")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // 4. Re-register with revoked token — should be rejected
        mvc.perform(post("/api/agents/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\": \"integration-agent\", \"token\": \"%s\", \"cpuCores\": 4, \"memoryMb\": 8192}"
                    .formatted(agentToken)))
            .andExpect(status().isUnauthorized());
    }
}
