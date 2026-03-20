package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByUsername() {
        var user = new User();
        user.setUsername("testuser");
        user.setPassword("hashedpassword");
        user.setRole(Role.OPERATOR);
        user.setEnabled(true);

        userRepository.save(user);

        var found = userRepository.findByUsername("testuser");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.OPERATOR);
        assertThat(found.get().isEnabled()).isTrue();
    }

    @Test
    void findByUsernameReturnsEmptyForMissing() {
        var found = userRepository.findByUsername("nonexistent");
        assertThat(found).isEmpty();
    }
}
