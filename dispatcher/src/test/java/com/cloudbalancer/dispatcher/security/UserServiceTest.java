package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({UserService.class, BCryptPasswordEncoder.class})
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authenticateWithValidCredentials() {
        userService.createUser("testuser", "password123", Role.OPERATOR);

        var result = userService.authenticate("testuser", "password123");
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.OPERATOR);
    }

    @Test
    void authenticateWithWrongPasswordReturnsEmpty() {
        userService.createUser("testuser2", "password123", Role.OPERATOR);

        var result = userService.authenticate("testuser2", "wrongpassword");
        assertThat(result).isEmpty();
    }

    @Test
    void authenticateWithNonexistentUserReturnsEmpty() {
        var result = userService.authenticate("nobody", "password");
        assertThat(result).isEmpty();
    }

    @Test
    void createUserHashesPassword() {
        userService.createUser("testuser3", "plaintext", Role.VIEWER);

        var user = userRepository.findByUsername("testuser3").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("plaintext");
        assertThat(new BCryptPasswordEncoder().matches("plaintext", user.getPassword())).isTrue();
    }
}
