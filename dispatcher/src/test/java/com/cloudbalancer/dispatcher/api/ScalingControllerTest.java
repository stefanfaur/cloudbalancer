package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.persistence.ScalingPolicyRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.service.ScalingPolicyService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class ScalingControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private ScalingPolicyRepository scalingPolicyRepository;
    @Autowired private ScalingPolicyService scalingPolicyService;
    @Autowired private WorkerRepository workerRepository;

    private String adminToken() {
        return jwtService.generateAccessToken("admin", Role.ADMIN);
    }

    private String viewerToken() {
        return jwtService.generateAccessToken("viewer", Role.VIEWER);
    }

    @BeforeEach
    void cleanUp() {
        scalingPolicyRepository.deleteAll();
        scalingPolicyService.reloadPolicy();
    }

    @Test
    void getScalingStatusReturnsDefaultPolicy() throws Exception {
        mvc.perform(get("/api/scaling/status")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.policy.minWorkers").value(2))
            .andExpect(jsonPath("$.policy.maxWorkers").value(20))
            .andExpect(jsonPath("$.runtimeMode").value("LOCAL"));
    }

    @Test
    void getScalingStatusRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/scaling/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotAccessScalingStatus() throws Exception {
        mvc.perform(get("/api/scaling/status")
                .header("Authorization", "Bearer " + viewerToken()))
            .andExpect(status().isForbidden());
    }

    @Test
    void updatePolicyPersistsAndReturns() throws Exception {
        var body = """
            {"minWorkers": 3, "maxWorkers": 10, "cooldownSeconds": 300,
             "scaleUpStep": 2, "scaleDownStep": 1, "drainTimeSeconds": 30}
            """;
        mvc.perform(put("/api/scaling/policy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.policy.minWorkers").value(3))
            .andExpect(jsonPath("$.policy.maxWorkers").value(10));

        // Verify persistence via GET
        mvc.perform(get("/api/scaling/status")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.policy.minWorkers").value(3))
            .andExpect(jsonPath("$.policy.maxWorkers").value(10));
    }

    @Test
    void triggerScaleUpAddsWorkerAndReturnsStatus() throws Exception {
        var body = """
            {"action": "SCALE_UP", "count": 1}
            """;
        mvc.perform(post("/api/scaling/trigger")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workerCount").isNumber())
            .andExpect(jsonPath("$.runtimeMode").value("LOCAL"));

        // Verify status shows lastDecision
        mvc.perform(get("/api/scaling/status")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastDecision").isNotEmpty())
            .andExpect(jsonPath("$.lastDecision.triggerType").value("MANUAL"));
    }

    @Test
    void invalidPolicyReturnsBadRequest() throws Exception {
        var body = """
            {"minWorkers": 10, "maxWorkers": 5, "cooldownSeconds": 300,
             "scaleUpStep": 1, "scaleDownStep": 1, "drainTimeSeconds": 30}
            """;
        mvc.perform(put("/api/scaling/policy")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
