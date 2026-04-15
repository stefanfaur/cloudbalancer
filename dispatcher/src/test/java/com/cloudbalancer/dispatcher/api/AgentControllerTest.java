package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void listAgents_empty_returns200() throws Exception {
        mvc.perform(get("/api/admin/agents")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAgent_notFound_returns404() throws Exception {
        mvc.perform(get("/api/admin/agents/unknown")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listAgents_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/admin/agents"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listAgents_asOperator_returns403() throws Exception {
        mvc.perform(get("/api/admin/agents")
                .header("Authorization", "Bearer " + operatorToken()))
            .andExpect(status().isForbidden());
    }

    private String adminToken() {
        return jwtService.generateAccessToken("admin", Role.ADMIN);
    }

    private String operatorToken() {
        return jwtService.generateAccessToken("operator", Role.OPERATOR);
    }
}
