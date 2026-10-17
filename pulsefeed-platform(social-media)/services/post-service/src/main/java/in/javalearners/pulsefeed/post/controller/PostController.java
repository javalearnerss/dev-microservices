package in.javalearners.pulsefeed.post.controller;

import java.net.URI;
import java.util.List;

import in.javalearners.pulsefeed.post.dto.CreatePostRequest;
import in.javalearners.pulsefeed.post.model.Post;
import in.javalearners.pulsefeed.post.service.PostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing the post endpoints.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    /**
     * Creates the controller.
     *
     * @param postService service that manages posts
     */
    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Creates a new post.
     *
     * @param request the validated post details
     * @return the created post with a Location header
     */
    @PostMapping
    public ResponseEntity<Post> create(@Valid @RequestBody CreatePostRequest request) {
        log.info("POST /api/posts - create post, authorId={}, content length={}", request.authorId(), request.content().length());
        Post post = postService.create(request);
        log.info("POST /api/posts - Post created successfully: id={}, authorId={}", post.id(), post.authorId());
        return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
    }

    /**
     * Lists posts, optionally restricted to specific authors.
     *
     * @param authorIds optional author identifiers to filter by; all posts are returned when absent
     * @return matching posts, newest first
     */
    @GetMapping
    public ResponseEntity<List<Post>> findAll(@RequestParam(required = false) List<String> authorIds) {
        log.info("GET /api/posts - Request received, authorIds filter present={}", authorIds != null);
        List<Post> posts = authorIds == null ? postService.findAll() : postService.findByAuthorIds(authorIds);
        log.info("GET /api/posts - Query completed, found {} posts", posts.size());
        return ResponseEntity.ok(posts);
    }

    /**
     * Finds a single post.
     *
     * @param id identifier of the post
     * @return the post
     * @throws ResponseStatusException with 404 if the post does not exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<Post> findById(@PathVariable String id) {
        log.info("GET /api/posts/{} - Request received, searching for post", id);
        Post post = postService.findById(id)
                .orElseThrow(() -> {
                    log.warn("GET /api/posts/{} - Post not found, returning 404", id);
                    return new ResponseStatusException(NOT_FOUND, "Post not found");
                });
        log.info("GET /api/posts/{} - Post found: authorId={}", id, post.authorId());
        return ResponseEntity.ok(post);
    }

    /**
     * Deletes a post.
     *
     * @param id identifier of the post
     * @return an empty 204 response
     * @throws ResponseStatusException with 404 if the post does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/posts/{} - Request received, attempting to delete post", id);
        if (!postService.delete(id)) {
            log.warn("DELETE /api/posts/{} - Post not found for deletion, returning 404", id);
            throw new ResponseStatusException(NOT_FOUND, "Post not found");
        }
        log.info("DELETE /api/posts/{} - Post deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets a post by ID with failure simulation capability.
     *
     * @param id identifier of the post
     * @return the post
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<Post> getPostById(@PathVariable String id) {
        log.info("GET /api/posts/{}/detail - Request received", id);
        Post post = postService.getPostById(id);
        log.info("GET /api/posts/{}/detail - Post retrieved: authorId={}", id, post.authorId());
        return ResponseEntity.ok(post);
    }

    /**
     * Gets multiple posts by IDs with failure simulation.
     *
     * @param ids comma-separated list of post identifiers
     * @return list of posts
     */
    @GetMapping("/batch")
    public ResponseEntity<List<Post>> getPostsByIds(@RequestParam String ids) {
        log.info("GET /api/posts/batch - Request received");
        List<String> postIds = List.of(ids.split(","));
        List<Post> posts = postService.getPostsByIds(postIds);
        log.info("GET /api/posts/batch - Retrieved {} posts", posts.size());
        return ResponseEntity.ok(posts);
    }

    /**
     * Gets post count by author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return post count
     */
    @GetMapping("/author/{authorId}/count")
    public ResponseEntity<Long> getPostCountByAuthor(@PathVariable String authorId) {
        log.info("GET /api/posts/author/{}/count - Request received", authorId);
        long count = postService.getPostCountByAuthor(authorId);
        log.info("GET /api/posts/author/{}/count - Author has {} posts", authorId, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Gets all posts by a specific author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return list of posts by this author
     */
    @GetMapping("/author/{authorId}/posts")
    public ResponseEntity<List<Post>> getPostsByAuthor(@PathVariable String authorId) {
        log.info("GET /api/posts/author/{}/posts - Request received", authorId);
        List<Post> posts = postService.getPostsByAuthor(authorId);
        log.info("GET /api/posts/author/{}/posts - Retrieved {} posts by author", posts.size(), authorId);
        return ResponseEntity.ok(posts);
    }
}
