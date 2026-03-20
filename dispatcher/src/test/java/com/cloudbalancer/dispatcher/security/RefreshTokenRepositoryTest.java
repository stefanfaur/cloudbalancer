package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByToken() {
        var user = new User();
        user.setUsername("tokenuser-" + UUID.randomUUID());
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
        assertThat(found.get().getUser().getUsername()).isEqualTo(user.getUsername());
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    void findByTokenReturnsEmptyForMissing() {
        var found = refreshTokenRepository.findByToken("nonexistent");
        assertThat(found).isEmpty();
    }
}
