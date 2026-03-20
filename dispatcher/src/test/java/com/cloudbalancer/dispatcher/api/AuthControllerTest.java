package com.cloudbalancer.dispatcher.api;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.api.dto.LoginRequest;
import com.cloudbalancer.dispatcher.api.dto.RefreshRequest;
import com.cloudbalancer.dispatcher.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;

    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        userService.createUser("logintest", "password123", Role.OPERATOR);

        String body = JsonUtil.mapper().writeValueAsString(new LoginRequest("logintest", "password123"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        String body = JsonUtil.mapper().writeValueAsString(new LoginRequest("nobody", "wrong"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithValidTokenReturnsNewPair() throws Exception {
        userService.createUser("refreshtest", "pass", Role.VIEWER);
        String loginBody = JsonUtil.mapper().writeValueAsString(new LoginRequest("refreshtest", "pass"));

        // Login first
        String loginResponse = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonUtil.mapper().readTree(loginResponse).get("refreshToken").asText();

        // Refresh
        String refreshBody = JsonUtil.mapper().writeValueAsString(new RefreshRequest(refreshToken));

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refreshWithInvalidTokenReturns401() throws Exception {
        String body = JsonUtil.mapper().writeValueAsString(new RefreshRequest("invalid-token"));

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotationInvalidatesOldToken() throws Exception {
        userService.createUser("rotatetest", "pass", Role.OPERATOR);
        String loginBody = JsonUtil.mapper().writeValueAsString(new LoginRequest("rotatetest", "pass"));

        // Login
        String loginResponse = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andReturn().getResponse().getContentAsString();
        String firstRefreshToken = JsonUtil.mapper().readTree(loginResponse).get("refreshToken").asText();

        // Refresh (rotates token)
        String refreshBody = JsonUtil.mapper().writeValueAsString(new RefreshRequest(firstRefreshToken));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk());

        // Try the old token again — should be revoked
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isUnauthorized());
    }
}
