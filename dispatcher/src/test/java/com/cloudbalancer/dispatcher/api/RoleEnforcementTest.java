package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class RoleEnforcementTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    private String tokenFor(String user, Role role) {
        return jwtService.generateAccessToken(user, role);
    }

    private String taskDescriptorJson() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        return JsonUtil.mapper().writeValueAsString(descriptor);
    }

    @Test
    void viewerCannotSubmitTasks() throws Exception {
        mvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("viewer", Role.VIEWER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskDescriptorJson()))
            .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanSubmitTasks() throws Exception {
        mvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("operator", Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskDescriptorJson()))
            .andExpect(status().isCreated());
    }

    @Test
    void viewerCanReadTasks() throws Exception {
        mvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("viewer", Role.VIEWER)))
            .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        var shortLived = new JwtService(
            "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZzEyMzQ1Njc4OTA=",
            0, 0
        );
        String expired = shortLived.generateAccessToken("user", Role.ADMIN);

        mvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + expired))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void apiClientCanSubmitTasks() throws Exception {
        mvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("apiclient", Role.API_CLIENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskDescriptorJson()))
            .andExpect(status().isCreated());
    }

    @Test
    void adminCanDoEverything() throws Exception {
        mvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("admin", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskDescriptorJson()))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + tokenFor("admin", Role.ADMIN)))
            .andExpect(status().isOk());
    }
}
