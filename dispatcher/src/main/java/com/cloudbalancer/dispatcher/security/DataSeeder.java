package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserService userService;

    public DataSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userService.hasUsers()) {
            userService.createUser("admin", "admin", Role.ADMIN);
            log.warn("Created default admin user (admin/admin) — change this in production!");
        }
    }
}
