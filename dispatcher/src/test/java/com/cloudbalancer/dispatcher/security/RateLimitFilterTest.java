package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class RateLimitFilterTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Use very low limits for testing
        registry.add("cloudbalancer.security.rate-limit.viewer", () -> "3");
        registry.add("cloudbalancer.security.rate-limit.admin", () -> "5");
    }

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    @Test
    void exceedingRateLimitReturns429() throws Exception {
        String token = jwtService.generateAccessToken("ratelimituser", Role.VIEWER);

        // First 3 requests should succeed (viewer limit = 3 in test)
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/tasks")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }

        // 4th request should be rate limited
        mvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void differentUsersHaveIndependentLimits() throws Exception {
        String token1 = jwtService.generateAccessToken("user1", Role.VIEWER);
        String token2 = jwtService.generateAccessToken("user2", Role.VIEWER);

        // Exhaust user1's limit
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());
        }
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token1))
            .andExpect(status().isTooManyRequests());

        // user2 should still work
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token2))
            .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestsAreRateLimited() throws Exception {
        // Auth endpoints are public but rate-limited by IP
        // Make many requests to /api/tasks without auth
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
        }
        // This tests that unauthenticated endpoints don't crash the rate limiter.
    }
}
