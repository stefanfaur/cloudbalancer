package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByToken() {
        var user = new User();
        user.setUsername("tokenuser");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user = userRepository.save(user);

        var token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(604800));
        token.setRevoked(false);

        refreshTokenRepository.save(token);

        var found = refreshTokenRepository.findByToken(token.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("tokenuser");
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    void findByTokenReturnsEmptyForMissing() {
        var found = refreshTokenRepository.findByToken("nonexistent");
        assertThat(found).isEmpty();
    }
}
