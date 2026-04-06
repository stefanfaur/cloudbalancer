package com.cloudbalancer.dispatcher.registration;

import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class AgentTokenServiceTest {

    @Autowired
    private AgentTokenService tokenService;

    @Autowired
    private AgentTokenRepository tokenRepository;

    @Test
    void createToken_generatesTokenWithPrefix_and_storesHash() {
        var result = tokenService.create("GPU rack", "admin");

        assertThat(result.token()).startsWith("cb_at_");
        assertThat(result.id()).isNotNull();
        assertThat(result.label()).isEqualTo("GPU rack");

        var entity = tokenRepository.findById(result.id()).orElseThrow();
        String expectedHash = AgentTokenService.sha256(result.token());
        assertThat(entity.getTokenHash()).isEqualTo(expectedHash);
    }

    @Test
    void validateToken_returnsTrue_forValidToken() {
        var result = tokenService.create("test-label", "admin");
        assertThat(tokenService.validate(result.token())).isTrue();
    }

    @Test
    void validateToken_returnsFalse_forInvalidToken() {
        assertThat(tokenService.validate("cb_at_bogus")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forRevokedToken() {
        var result = tokenService.create("revoke-test", "admin");
        tokenService.revoke(result.id());
        assertThat(tokenService.validate(result.token())).isFalse();
    }

    @Test
    void revokeToken_setsRevokedTrue() {
        var result = tokenService.create("revoke-flag-test", "admin");
        tokenService.revoke(result.id());
        var entity = tokenRepository.findById(result.id()).orElseThrow();
        assertThat(entity.isRevoked()).isTrue();
    }

    @Test
    void validateToken_updatesLastUsedAt() {
        var result = tokenService.create("last-used-test", "admin");
        var before = tokenRepository.findById(result.id()).orElseThrow();
        assertThat(before.getLastUsedAt()).isNull();

        tokenService.validate(result.token());

        var after = tokenRepository.findById(result.id()).orElseThrow();
        assertThat(after.getLastUsedAt()).isNotNull();
    }
}
