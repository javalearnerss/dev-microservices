package com.learn.kube.deployment.user.management.controller;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    // userId -> user details (in-memory)
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    @PostConstruct
    void loadDummyUsers() {
        logger.info("Loading dummy users into in-memory store...");
        users.put("U-1001", new User("U-1001", "Alice", "alice@test.com", Instant.now().toString()));
        users.put("U-1002", new User("U-1002", "Bob", "bob@test.com", Instant.now().toString()));
        users.put("U-1003", new User("U-1003", "Charlie", "charlie@test.com", Instant.now().toString()));
        logger.info("Dummy users loaded. count={}", users.size());
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getUsers() {
        logger.info("GET /users/all called");
        List<User> usrs = new ArrayList<>();
        users.forEach((id, user) -> usrs.add(user));
        logger.info("Returning all users. count={}", usrs.size());
        return ResponseEntity.ok(usrs);
    }

    /**
     * Creates a new user.
     * Example: POST /users?name=Sandeep&email=sandeep@test.com
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestParam String name,
                                        @RequestParam String email) {

        logger.info("POST /users called. name='{}', email='{}'", name, email);

        if (name.isBlank() || email.isBlank()) {
            logger.warn("Create user failed: name/email blank");
            return bad("name and email are required");
        }

        String userId = UUID.randomUUID().toString();
        User user = new User(userId, name, email, Instant.now().toString());

        users.put(userId, user);

        logger.info("User created successfully. userId={}", userId);

        return ResponseEntity.ok(Map.of(
                "userId", user.userId,
                "name", user.name,
                "email", user.email
        ));
    }

    /**
     * Fetch user details by userId.
     * Example: GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        logger.info("GET /users/{} called", userId);

        User user = users.get(userId);
        if (user == null) {
            logger.warn("User not found. userId={}", userId);
            return ResponseEntity.notFound().build();
        }

        logger.info("User found. userId={}, name='{}'", userId, user.name);

        return ResponseEntity.ok(Map.of(
                "userId", user.userId,
                "name", user.name,
                "email", user.email,
                "createdAt", user.createdAt
        ));
    }

    /**
     * Updates user name or email.
     * Example: PUT /users/{userId}?name=NewName&email=new@test.com
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String email) {

        logger.info("PUT /users/{} called. name='{}', email='{}'", userId, name, email);

        User existing = users.get(userId);
        if (existing == null) {
            logger.warn("Update failed: user not found. userId={}", userId);
            return bad("user not found");
        }

        User updated = new User(
                userId,
                name != null ? name : existing.name,
                email != null ? email : existing.email,
                existing.createdAt
        );

        users.put(userId, updated);

        logger.info("User updated successfully. userId={}", userId);

        return ResponseEntity.ok(Map.of(
                "userId", updated.userId,
                "name", updated.name,
                "email", updated.email
        ));
    }

    /**
     * Deletes a user.
     * Example: DELETE /users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        logger.info("DELETE /users/{} called", userId);

        User removed = users.remove(userId);
        if (removed == null) {
            logger.warn("Delete failed: user not found. userId={}", userId);
            return bad("user not found");
        }

        logger.info("User deleted successfully. userId={}", userId);

        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "userId", userId
        ));
    }

    private ResponseEntity<Map<String, String>> bad(String msg) {
        logger.warn("Bad request: {}", msg);
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private static class User {
        final String userId;
        final String name;
        final String email;
        final String createdAt;

        User(String userId, String name, String email, String createdAt) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.createdAt = createdAt;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getCreatedAt() { return createdAt; }
    }
}
