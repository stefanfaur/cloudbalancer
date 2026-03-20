package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.security.RateLimitFilter;
import com.cloudbalancer.dispatcher.security.RateLimitProperties;
import com.cloudbalancer.dispatcher.security.RefreshTokenRepository;
import com.cloudbalancer.dispatcher.security.UserService;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({RateLimitFilter.class, RateLimitProperties.class})
class TaskControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private TaskService taskService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void submitTaskReturns201WithEnvelope() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope envelope = TaskEnvelope.create(descriptor);
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        when(taskService.submitTask(any())).thenReturn(envelope);

        String body = JsonUtil.mapper().writeValueAsString(descriptor);

        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(envelope.getId().toString()))
            .andExpect(jsonPath("$.state").value("QUEUED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void submitTaskWithNullExecutorTypeReturns400() throws Exception {
        String body = """
            {"executionSpec": {"durationMs": 1000}, "priority": "NORMAL"}
            """;

        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getTaskByIdReturnsTask() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope envelope = TaskEnvelope.create(descriptor);
        when(taskService.getTask(envelope.getId())).thenReturn(envelope);

        mvc.perform(get("/api/tasks/" + envelope.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(envelope.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getTaskByNonexistentIdReturns404() throws Exception {
        when(taskService.getTask(any())).thenReturn(null);

        mvc.perform(get("/api/tasks/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listTasksReturnsAll() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope e1 = TaskEnvelope.create(descriptor);
        TaskEnvelope e2 = TaskEnvelope.create(descriptor);
        when(taskService.listTasks()).thenReturn(List.of(e1, e2));

        mvc.perform(get("/api/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
