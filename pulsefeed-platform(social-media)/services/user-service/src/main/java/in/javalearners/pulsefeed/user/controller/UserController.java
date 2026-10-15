package in.javalearners.pulsefeed.user.controller;

import java.net.URI;
import java.util.List;

import in.javalearners.pulsefeed.user.dto.CreateUserRequest;
import in.javalearners.pulsefeed.user.model.User;
import in.javalearners.pulsefeed.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing the user and follow endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Creates the controller.
     *
     * @param userService service that manages users
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param request the validated user details
     * @return the created user with a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users - create user, email={}, name={}", request.email(), request.name());
        User user = userService.create(request);
        log.info("POST /api/users - User created successfully: id={}, name={}, email={}", user.id(), user.name(), user.email());
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }

    /**
     * Lists all users.
     *
     * @return all users ordered by identifier
     */
    @GetMapping
    public List<User> findAll() {
        log.info("GET /api/users - list all users - Request received");
        List<User> users = userService.findAll();
        log.info("GET /api/users - Query completed, found {} total users", users.size());
        return users;
    }

    /**
     * Finds a single user.
     *
     * @param id identifier of the user
     * @return the user
     * @throws ResponseStatusException with 404 if the user does not exist
     */
    @GetMapping("/{id}")
    public User findById(@PathVariable String id) {
        log.info("GET /api/users/{} - Request received, searching for user", id);
        return userService.findById(id)
                .map(user -> {
                    log.info("GET /api/users/{} - User found: name={}, email={}", id, user.name(), user.email());
                    return user;
                })
                .orElseThrow(() -> {
                    log.warn("GET /api/users/{} - User not found, returning 404", id);
                    return new ResponseStatusException(NOT_FOUND, "User not found");
                });
    }

    /**
     * Makes a user follow another user.
     *
     * @param id       identifier of the follower
     * @param targetId identifier of the user to follow
     * @return an empty 204 response
     */
    @PutMapping("/{id}/following/{targetId}")
    public ResponseEntity<Void> follow(@PathVariable String id, @PathVariable String targetId) {
        log.info("PUT /api/users/{}/following/{} - Request received, user {} following user {}", id, targetId, id, targetId);
        userService.follow(id, targetId);
        log.info("PUT /api/users/{}/following/{} - Follow operation completed successfully", id, targetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Makes a user stop following another user.
     *
     * @param id       identifier of the follower
     * @param targetId identifier of the user to unfollow
     * @return an empty 204 response
     */
    @DeleteMapping("/{id}/following/{targetId}")
    public ResponseEntity<Void> unfollow(@PathVariable String id, @PathVariable String targetId) {
        log.info("DELETE /api/users/{}/following/{} - Request received, user {} unfollowing user {}", id, targetId, id, targetId);
        userService.unfollow(id, targetId);
        log.info("DELETE /api/users/{}/following/{} - Unfollow operation completed successfully", id, targetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists the users that a user follows.
     *
     * @param id identifier of the follower
     * @return the followed users ordered by identifier
     */
    @GetMapping("/{id}/following")
    public List<User> findFollowing(@PathVariable String id) {
        log.info("GET /api/users/{}/following - Request received, retrieving following list for user {}", id, id);
        List<User> following = userService.findFollowing(id);
        log.info("GET /api/users/{}/following - Query completed, user {} is following {} users: {}", id, id, following.size(), following.stream().map(User::id).toList());
        return following;
    }

    /**
     * Deletes a user.
     *
     * @param id identifier of the user
     * @return an empty 204 response
     * @throws ResponseStatusException with 404 if the user does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/users/{} - Request received, attempting to delete user", id);
        if (!userService.delete(id)) {
            log.warn("DELETE /api/users/{} - User not found for deletion, returning 404", id);
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
        log.info("DELETE /api/users/{} - User deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
