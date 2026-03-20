package com.cloudbalancer.metrics.security;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.metrics.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class MetricsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/metrics/workers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtReturns200() throws Exception {
        String token = jwtService.generateAccessToken("testuser", Role.ADMIN);

        mockMvc.perform(get("/api/metrics/workers")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void expiredJwtReturns401() throws Exception {
        String token = jwtService.generateExpiredToken("testuser", Role.ADMIN);

        mockMvc.perform(get("/api/metrics/workers")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/metrics/workers")
                .header("Authorization", "Bearer not.a.valid.token"))
            .andExpect(status().isUnauthorized());
    }
}
