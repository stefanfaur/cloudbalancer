package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.api.dto.BulkTaskRequest;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.security.UserService;
import com.cloudbalancer.dispatcher.service.TaskService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class TaskControllerPaginationTest {

    @Autowired private MockMvc mvc;
    @Autowired private TaskService taskService;
    @Autowired private UserService userService;
    @Autowired private JwtService jwtService;

    private String operatorToken;

    @BeforeEach
    void setUp() {
        try {
            userService.createUser("paginationtest", "pass", Role.OPERATOR);
        } catch (Exception ignored) {}
        operatorToken = jwtService.generateAccessToken("paginationtest", Role.OPERATOR);
    }

    private TaskEnvelope submitSimulated() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        return taskService.submitTask(descriptor);
    }

    @Test
    void paginationReturnsCorrectSlice() throws Exception {
        // Create 10 tasks
        for (int i = 0; i < 10; i++) submitSimulated();

        mvc.perform(get("/api/tasks?offset=0&limit=3")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks.length()").value(3))
            .andExpect(jsonPath("$.limit").value(3))
            .andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void filterByStatusReturnsMatchingTasks() throws Exception {
        submitSimulated(); // QUEUED state

        mvc.perform(get("/api/tasks?status=QUEUED")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.tasks[0].state").value("QUEUED"));
    }

    @Test
    void bulkCancelCancelsRunningTasks() throws Exception {
        var e1 = submitSimulated();
        var e2 = submitSimulated();

        String body = JsonUtil.mapper().writeValueAsString(
            new BulkTaskRequest(List.of(e1.getId(), e2.getId())));

        mvc.perform(post("/api/tasks/bulk/cancel")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].success").value(true))
            .andExpect(jsonPath("$[1].success").value(true));

        // Verify tasks are cancelled
        mvc.perform(get("/api/tasks/" + e1.getId())
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(jsonPath("$.state").value("CANCELLED"));
    }

    @Test
    void bulkRetryRequeuesFailedTasks() throws Exception {
        var e1 = submitSimulated();
        // Manually transition to a state we can fail from
        var record = taskService.getTaskRecord(e1.getId());
        record.transitionTo(TaskState.ASSIGNED);
        record.transitionTo(TaskState.PROVISIONING);
        record.transitionTo(TaskState.RUNNING);
        record.transitionTo(TaskState.FAILED);
        taskService.updateTask(record);

        String body = JsonUtil.mapper().writeValueAsString(
            new BulkTaskRequest(List.of(e1.getId())));

        mvc.perform(post("/api/tasks/bulk/retry")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].success").value(true));

        // Verify task is re-queued
        mvc.perform(get("/api/tasks/" + e1.getId())
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(jsonPath("$.state").value("QUEUED"));
    }
}
