package com.example.qrredirect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.qrredirect.repo.UserRepository;
import com.example.qrredirect.service.UserService;

@Component
@Profile("prod")
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository repo;
    private final UserService users;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository repo, UserService users,
                          @Value("${ADMIN_EMAIL:}") String email,
                          @Value("${ADMIN_PASSWORD:}") String password) {
        this.repo = repo;
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Only ever bootstraps an empty database, so restarts never add or change accounts
        if (repo.count() > 0 || email.isBlank() || password.isBlank()) {
            return;
        }
        users.create("Admin", email, password);
        log.info("Created initial admin user {}", email);
    }
}