package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.api.dto.WorkerTagsRequest;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AdminControllerWorkerTagsTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private WorkerRegistryService workerRegistryService;

    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateAccessToken("admin", Role.ADMIN);
        operatorToken = jwtService.generateAccessToken("operator", Role.OPERATOR);

        workerRegistryService.registerWorker("tag-test-worker", WorkerHealthState.HEALTHY,
            new WorkerCapabilities(
                Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 8192, 10240, false, 0, false),
                Set.of("initial-tag")
            ));
    }

    @Test
    void adminCanUpdateWorkerTags() throws Exception {
        var request = new WorkerTagsRequest(Set.of("gpu", "high-memory"));
        mvc.perform(put("/api/admin/workers/tag-test-worker/tags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unknownWorkerReturns404() throws Exception {
        var request = new WorkerTagsRequest(Set.of("tag"));
        mvc.perform(put("/api/admin/workers/nonexistent/tags")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void operatorCannotUpdateTags() throws Exception {
        var request = new WorkerTagsRequest(Set.of("tag"));
        mvc.perform(put("/api/admin/workers/tag-test-worker/tags")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.mapper().writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
