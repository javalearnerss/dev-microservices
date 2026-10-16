package in.javalearners.pulsefeed.post.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import in.javalearners.pulsefeed.post.dto.CreatePostRequest;
import in.javalearners.pulsefeed.post.model.Post;
import in.javalearners.pulsefeed.post.util.FailureSimulator;
import org.springframework.stereotype.Service;

/**
 * In-memory store and business logic for posts.
 */
@Service
public class PostService {

    private final ConcurrentHashMap<String, Post> posts = new ConcurrentHashMap<>();
    private final FailureSimulator failureSimulator;

    public PostService(FailureSimulator failureSimulator) {
        this.failureSimulator = failureSimulator;
    }

    /**
     * Creates and stores a post with a generated identifier and the current timestamp.
     *
     * @param request the post details
     * @return the stored post
     */
    public Post create(CreatePostRequest request) {
        Post post = new Post(
                UUID.randomUUID().toString(),
                request.authorId(),
                request.content(),
                Instant.now()
        );
        posts.put(post.id(), post);
        return post;
    }

    /**
     * Returns all posts.
     *
     * @return all posts, newest first
     */
    public List<Post> findAll() {
        return posts.values().stream()
                .sorted(Comparator.comparing(Post::createdAt).reversed())
                .toList();
    }

    /**
     * Returns the posts written by any of the given authors.
     * This method integrates with FailureSimulator for testing resilience patterns.
     *
     * @param authorIds identifiers of the authors
     * @return matching posts, newest first
     */
    public List<Post> findByAuthorIds(Collection<String> authorIds) {
        try {
            failureSimulator.simulateCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulated slowness interrupted", e);
        }
        
        return posts.values().stream()
                .filter(post -> authorIds.contains(post.authorId()))
                .sorted(Comparator.comparing(Post::createdAt).reversed())
                .toList();
    }

    /**
     * Finds a post by its identifier.
     *
     * @param id identifier of the post
     * @return the post, or empty if none exists
     */
    public Optional<Post> findById(String id) {
        return Optional.ofNullable(posts.get(id));
    }

    /**
     * Gets post by ID with failure simulation.
     *
     * @param id identifier of the post
     * @return the post if found
     */
    public Post getPostById(String id) {
        try {
            failureSimulator.simulateCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulated slowness interrupted", e);
        }
        
        return findById(id).orElseThrow(() -> new RuntimeException("Post not found: " + id));
    }

    /**
     * Gets multiple posts by IDs with failure simulation.
     *
     * @param postIds list of post identifiers
     * @return list of posts
     */
    public List<Post> getPostsByIds(List<String> postIds) {
        try {
            failureSimulator.simulateCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulated slowness interrupted", e);
        }
        
        return postIds.stream()
                .map(posts::get)
                .filter(post -> post != null)
                .sorted(Comparator.comparing(Post::createdAt).reversed())
                .toList();
    }

    /**
     * Gets post count by author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return count of posts by this author
     */
    public long getPostCountByAuthor(String authorId) {
        try {
            failureSimulator.simulateCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulated slowness interrupted", e);
        }
        
        return posts.values().stream()
                .filter(post -> post.authorId().equals(authorId))
                .count();
    }

    /**
     * Gets posts by author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return posts by this author, newest first
     */
    public List<Post> getPostsByAuthor(String authorId) {
        try {
            failureSimulator.simulateCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulated slowness interrupted", e);
        }
        
        return posts.values().stream()
                .filter(post -> post.authorId().equals(authorId))
                .sorted(Comparator.comparing(Post::createdAt).reversed())
                .toList();
    }

    /**
     * Deletes a post.
     *
     * @param id identifier of the post
     * @return {@code true} if a post was removed, {@code false} if none existed
     */
    public boolean delete(String id) {
        return posts.remove(id) != null;
    }
}
