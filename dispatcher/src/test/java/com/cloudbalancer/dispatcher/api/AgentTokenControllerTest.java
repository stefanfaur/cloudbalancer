package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.registration.AgentTokenService;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AgentTokenControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AgentTokenService tokenService;

    private String adminToken() {
        return jwtService.generateAccessToken("admin", Role.ADMIN);
    }

    private String operatorToken() {
        return jwtService.generateAccessToken("operator", Role.OPERATOR);
    }

    @Test
    void createToken_asAdmin_returns200WithToken() throws Exception {
        mvc.perform(post("/api/admin/agent-tokens")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\": \"GPU rack\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.label").value("GPU rack"));
    }

    @Test
    void listTokens_asAdmin_returnsCreatedToken() throws Exception {
        tokenService.create("list-test", "admin");

        mvc.perform(get("/api/admin/agent-tokens")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.label == 'list-test')]").exists());
    }

    @Test
    void revokeToken_asAdmin_returns204() throws Exception {
        var result = tokenService.create("revoke-test", "admin");

        mvc.perform(post("/api/admin/agent-tokens/" + result.id() + "/revoke")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void createToken_asOperator_returns403() throws Exception {
        mvc.perform(post("/api/admin/agent-tokens")
                .header("Authorization", "Bearer " + operatorToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\": \"should-fail\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listTokens_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/admin/agent-tokens"))
            .andExpect(status().isUnauthorized());
    }
}
