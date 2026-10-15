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
import org.springframework.stereotype.Service;

/**
 * In-memory store and business logic for posts.
 */
@Service
public class PostService {

    private final ConcurrentHashMap<String, Post> posts = new ConcurrentHashMap<>();

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
     *
     * @param authorIds identifiers of the authors
     * @return matching posts, newest first
     */
    public List<Post> findByAuthorIds(Collection<String> authorIds) {
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
     * Deletes a post.
     *
     * @param id identifier of the post
     * @return {@code true} if a post was removed, {@code false} if none existed
     */
    public boolean delete(String id) {
        return posts.remove(id) != null;
    }
}
