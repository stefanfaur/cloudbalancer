package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
            .filter(User::isEnabled)
            .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    public User createUser(String username, String rawPassword, Role role) {
        var user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public boolean hasUsers() {
        return userRepository.count() > 0;
    }
}
