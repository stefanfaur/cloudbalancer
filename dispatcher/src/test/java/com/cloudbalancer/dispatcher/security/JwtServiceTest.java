package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 256-bit secret for HMAC-SHA256 (base64-encoded, at least 32 bytes)
        jwtService = new JwtService(
            "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZzEyMzQ1Njc4OTA=",
            900,    // 15 min access token
            604800  // 7 days refresh token
        );
    }

    @Test
    void generateTokenContainsExpectedClaims() {
        String token = jwtService.generateAccessToken("admin", Role.ADMIN);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.ADMIN);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() {
        // Create a service with 0-second expiry for testing
        var shortLived = new JwtService(
            "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZzEyMzQ1Njc4OTA=",
            0, 0
        );
        String token = shortLived.generateAccessToken("admin", Role.ADMIN);

        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateAccessToken("admin", Role.ADMIN);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void differentRolesProduceDifferentClaims() {
        String adminToken = jwtService.generateAccessToken("user1", Role.ADMIN);
        String viewerToken = jwtService.generateAccessToken("user2", Role.VIEWER);

        assertThat(jwtService.extractRole(adminToken)).isEqualTo(Role.ADMIN);
        assertThat(jwtService.extractRole(viewerToken)).isEqualTo(Role.VIEWER);
    }
}
