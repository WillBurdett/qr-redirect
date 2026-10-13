package com.example.qrredirect.config;

import com.example.qrredirect.repo.UserRepository;
import com.example.qrredirect.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevSeed implements ApplicationRunner {
    private final UserRepository repo;
    private final UserService users;

    public DevSeed(UserRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!repo.existsByEmail("admin@qr.com")) {
            users.create("Admin", "admin@qr.com", "password123");
        }
    }
}