package com.learn.kube.deployment.user.management.controller;


import jakarta.annotation.PostConstruct;
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

    // userId -> user details (in-memory)
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    @PostConstruct
    void loadDummyUsers() {
        users.put("U-1001", new User("U-1001", "Alice", "alice@test.com", Instant.now().toString()));
        users.put("U-1002", new User("U-1002", "Bob", "bob@test.com", Instant.now().toString()));
        users.put("U-1003", new User("U-1003", "Charlie", "charlie@test.com", Instant.now().toString()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getUsers() {
        List<User> usrs = new ArrayList<>();
        users.forEach((id, user)-> {
            usrs.add(user);
        });
        return ResponseEntity.ok(usrs);
    }

    /**
     * Creates a new user.
     * Called when a user signs up in the ticket booking system.
     *
     * Example:
     * POST /users?name=Sandeep&email=sandeep@test.com
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestParam String name,
                                        @RequestParam String email) {

        if (name.isBlank() || email.isBlank())
            return bad("name and email are required");

        String userId = UUID.randomUUID().toString();
        User user = new User(userId, name, email, Instant.now().toString());

        users.put(userId, user);

        return ResponseEntity.ok(Map.of(
                "userId", user.userId,
                "name", user.name,
                "email", user.email
        ));
    }

    /**
     * Fetch user details by userId.
     * Booking Service can use this to validate the user.
     *
     * Example:
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        User user = users.get(userId);
        if (user == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(Map.of(
                "userId", user.userId,
                "name", user.name,
                "email", user.email,
                "createdAt", user.createdAt
        ));
    }

    /**
     * Updates user name or email.
     * Used when user edits profile.
     *
     * Example:
     * PUT /users/{userId}?name=NewName&email=new@test.com
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String email) {

        User existing = users.get(userId);
        if (existing == null) return bad("user not found");

        User updated = new User(
                userId,
                name != null ? name : existing.name,
                email != null ? email : existing.email,
                existing.createdAt
        );

        users.put(userId, updated);

        return ResponseEntity.ok(Map.of(
                "userId", updated.userId,
                "name", updated.name,
                "email", updated.email
        ));
    }

    /**
     * Deletes a user.
     * Rare in real systems, but useful for demo/testing.
     *
     * Example:
     * DELETE /users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        User removed = users.remove(userId);
        if (removed == null) return bad("user not found");

        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "userId", userId
        ));
    }

    /**
     * Helper method to return consistent bad request responses.
     */
    private ResponseEntity<Map<String, String>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    /**
     * In-memory user model.
     */
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

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getCreatedAt() {
            return createdAt;
        }

    }
}

