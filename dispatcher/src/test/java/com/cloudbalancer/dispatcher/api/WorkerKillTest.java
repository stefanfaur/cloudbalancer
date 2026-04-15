package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.security.JwtService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class WorkerKillTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WorkerRegistryService workerRegistryService;

    @Test
    void killWorker_asAdmin_existingWorker_returns204() throws Exception {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of());
        workerRegistryService.registerWorker("kill-test-1", WorkerHealthState.HEALTHY, caps);

        mvc.perform(delete("/api/admin/workers/kill-test-1")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isNoContent());

        var worker = workerRegistryService.getWorker("kill-test-1");
        assertThat(worker).isNotNull();
        assertThat(worker.getHealthState()).isEqualTo(WorkerHealthState.STOPPING);
    }

    @Test
    void killWorker_notFound_returns404() throws Exception {
        mvc.perform(delete("/api/admin/workers/nonexistent-worker")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void killWorker_alreadyStopping_returns409() throws Exception {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of());
        workerRegistryService.registerWorker("kill-test-2", WorkerHealthState.HEALTHY, caps);
        workerRegistryService.killWorker("kill-test-2");

        mvc.perform(delete("/api/admin/workers/kill-test-2")
                .header("Authorization", "Bearer " + adminToken()))
            .andExpect(status().isConflict());
    }

    @Test
    void killWorker_asOperator_returns403() throws Exception {
        mvc.perform(delete("/api/admin/workers/any-worker")
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
