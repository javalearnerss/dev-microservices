package in.javalearners.pulsefeed.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import in.javalearners.pulsefeed.user.dto.CreateUserRequest;
import in.javalearners.pulsefeed.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * In-memory store and business logic for users and follow relationships.
 */
@Service
public class UserService {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> following = new ConcurrentHashMap<>();

    /**
     * Creates and stores a user with a generated identifier.
     *
     * @param request the user details
     * @return the stored user
     */
    public User create(CreateUserRequest request) {
        User user = new User(UUID.randomUUID().toString(), request.name(), request.email());
        users.put(user.id(), user);
        return user;
    }

    /**
     * Stores a user with a fixed identifier unless one already exists with that identifier.
     *
     * @param id    the fixed identifier
     * @param name  display name of the user
     * @param email email address of the user
     * @return the stored user
     */
    public User seed(String id, String name, String email) {
        User user = new User(id, name, email);
        users.putIfAbsent(id, user);
        return users.get(id);
    }

    /**
     * Returns all users.
     *
     * @return all users ordered by identifier
     */
    public List<User> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::id))
                .toList();
    }

    /**
     * Finds a user by identifier.
     *
     * @param id identifier of the user
     * @return the user, or empty if none exists
     */
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    /**
     * Deletes a user and removes every follow relationship involving them.
     *
     * @param id identifier of the user
     * @return {@code true} if a user was removed, {@code false} if none existed
     */
    public boolean delete(String id) {
        boolean removed = users.remove(id) != null;
        if (removed) {
            following.remove(id);
            following.values().forEach(ids -> ids.remove(id));
        }
        return removed;
    }

    /**
     * Makes a user follow another user.
     *
     * @param userId   identifier of the follower
     * @param targetId identifier of the user to follow
     * @throws ResponseStatusException with 404 if either user does not exist,
     *                                 or 400 if a user tries to follow themselves
     */
    public void follow(String userId, String targetId) {
        requireExists(userId);
        requireExists(targetId);
        if (userId.equals(targetId)) {
            throw new ResponseStatusException(BAD_REQUEST, "A user cannot follow themselves");
        }
        following.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(targetId);
    }

    /**
     * Makes a user stop following another user.
     *
     * @param userId   identifier of the follower
     * @param targetId identifier of the user to unfollow
     * @throws ResponseStatusException with 404 if the follower does not exist
     */
    public void unfollow(String userId, String targetId) {
        requireExists(userId);
        Set<String> ids = following.get(userId);
        if (ids != null) {
            ids.remove(targetId);
        }
    }

    /**
     * Lists the users that a user follows.
     *
     * @param userId identifier of the follower
     * @return the followed users ordered by identifier
     * @throws ResponseStatusException with 404 if the follower does not exist
     */
    public List<User> findFollowing(String userId) {
        requireExists(userId);
        return following.getOrDefault(userId, Set.of()).stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(User::id))
                .toList();
    }

    /**
     * Ensures a user exists.
     *
     * @param id identifier of the user
     * @throws ResponseStatusException with 404 if the user does not exist
     */
    private void requireExists(String id) {
        if (!users.containsKey(id)) {
            throw new ResponseStatusException(NOT_FOUND, "User not found: " + id);
        }
    }
}
