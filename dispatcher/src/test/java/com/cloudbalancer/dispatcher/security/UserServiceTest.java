package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authenticateWithValidCredentials() {
        String username = "testuser-" + UUID.randomUUID();
        userService.createUser(username, "password123", Role.OPERATOR);

        var result = userService.authenticate(username, "password123");
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.OPERATOR);
    }

    @Test
    void authenticateWithWrongPasswordReturnsEmpty() {
        String username = "testuser2-" + UUID.randomUUID();
        userService.createUser(username, "password123", Role.OPERATOR);

        var result = userService.authenticate(username, "wrongpassword");
        assertThat(result).isEmpty();
    }

    @Test
    void authenticateWithNonexistentUserReturnsEmpty() {
        var result = userService.authenticate("nobody", "password");
        assertThat(result).isEmpty();
    }

    @Test
    void createUserHashesPassword() {
        String username = "testuser3-" + UUID.randomUUID();
        userService.createUser(username, "plaintext", Role.VIEWER);

        var user = userRepository.findByUsername(username).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("plaintext");
        assertThat(new BCryptPasswordEncoder().matches("plaintext", user.getPassword())).isTrue();
    }
}
