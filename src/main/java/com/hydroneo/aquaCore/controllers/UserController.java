package com.hydroneo.aquaCore.controllers;

import com.hydroneo.aquaCore.models.User;
import com.hydroneo.aquaCore.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/from-oauth")
    public Mono<User> fromOAuth(@RequestBody User user) {
        log.info("🟢 Received from OAuth: {}", user);
        return userService.createOrUpdate(user);
    }

    @GetMapping("/user-info")
    public Mono<User> getUserInfo(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username
    ) {
        log.info("🔐 Received token: {}", token);
        log.info("👤 Requested username: {}", username);
        return userService.getUserByUsername(username);
    }
}
