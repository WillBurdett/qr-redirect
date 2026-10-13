package com.example.qrredirect.web;

import com.example.qrredirect.model.User;
import com.example.qrredirect.service.UserService;
import com.example.qrredirect.web.AuthDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String SESSION_USER_ID = "userId";

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        User user = users.login(req.email(), req.password());
        startSession(http, user);
        return UserResponse.of(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        if (!(session.getAttribute(SESSION_USER_ID) instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }
        return UserResponse.of(users.get(id));
    }

    private static void startSession(HttpServletRequest http, User user) {
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate();   // fresh session id on login
        http.getSession(true).setAttribute(SESSION_USER_ID, user.getId());
    }
}