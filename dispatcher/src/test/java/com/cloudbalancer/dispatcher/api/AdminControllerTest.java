package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.api.dto.StrategyRequest;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AdminControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    private String adminToken() {
        return jwtService.generateAccessToken("admin", Role.ADMIN);
    }

    private String operatorToken() {
        return jwtService.generateAccessToken("operator", Role.OPERATOR);
    }

    @Test
    void adminCanGetCurrentStrategy() throws Exception {
        mvc.perform(get("/api/admin/strategy")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strategy").isNotEmpty());
    }

    @Test
    void adminCanSwitchToPreset() throws Exception {
        var request = new StrategyRequest("LEAST_CONNECTIONS", null);
        mvc.perform(put("/api/admin/strategy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strategy").value("LEAST_CONNECTIONS"));
    }

    @Test
    void adminCanSwitchToCustomWeights() throws Exception {
        var request = new StrategyRequest("CUSTOM",
            Map.of("resourceAvailability", 60, "queueDepth", 40));
        mvc.perform(put("/api/admin/strategy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strategy").value("CUSTOM"))
            .andExpect(jsonPath("$.weights.resourceAvailability").value(60))
            .andExpect(jsonPath("$.weights.queueDepth").value(40));
    }

    @Test
    void adminCanSwitchToRoundRobin() throws Exception {
        var request = new StrategyRequest("ROUND_ROBIN", null);
        mvc.perform(put("/api/admin/strategy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strategy").value("ROUND_ROBIN"));
    }

    @Test
    void invalidStrategyReturnsBadRequest() throws Exception {
        var request = new StrategyRequest("NONEXISTENT", null);
        mvc.perform(put("/api/admin/strategy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void operatorCannotSwitchStrategy() throws Exception {
        var request = new StrategyRequest("ROUND_ROBIN", null);
        mvc.perform(put("/api/admin/strategy")
                .header("Authorization", "Bearer " + operatorToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotGetStrategy() throws Exception {
        mvc.perform(get("/api/admin/strategy")
                .header("Authorization", "Bearer " + operatorToken()))
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccess() throws Exception {
        mvc.perform(get("/api/admin/strategy"))
            .andExpect(status().isUnauthorized());
    }
}
