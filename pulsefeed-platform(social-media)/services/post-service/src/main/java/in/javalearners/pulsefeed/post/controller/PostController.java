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
     * @return the created post with a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<Post> create(@Valid @RequestBody CreatePostRequest request) {
        log.info("POST /api/posts - create post, authorId={}, content length={}", request.authorId(), request.content().length());
        Post post = postService.create(request);
        log.info("POST /api/posts - Post created successfully: id={}, authorId={}, content preview={}", post.id(), post.authorId(), post.content().substring(0, Math.min(50, post.content().length())));
        return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
    }

    /**
     * Lists posts, optionally restricted to specific authors.
     *
     * @param authorIds optional author identifiers to filter by; all posts are returned when absent
     * @return matching posts, newest first
     */
    @GetMapping
    public List<Post> findAll(@RequestParam(required = false) List<String> authorIds) {
        log.info("GET /api/posts - Request received, listing posts with authorIds filter: {}", authorIds);
        List<Post> posts = authorIds == null ? postService.findAll() : postService.findByAuthorIds(authorIds);
        log.info("GET /api/posts - Query completed, found {} posts", posts.size());
        return posts;
    }

    /**
     * Finds a single post.
     *
     * @param id identifier of the post
     * @return the post
     * @throws ResponseStatusException with 404 if the post does not exist
     */
    @GetMapping("/{id}")
    public Post findById(@PathVariable String id) {
        log.info("GET /api/posts/{} - Request received, searching for post", id);
        return postService.findById(id)
                .map(post -> {
                    log.info("GET /api/posts/{} - Post found: authorId={}, content preview={}", id, post.authorId(), post.content().substring(0, Math.min(50, post.content().length())));
                    return post;
                })
                .orElseThrow(() -> {
                    log.warn("GET /api/posts/{} - Post not found, returning 404", id);
                    return new ResponseStatusException(NOT_FOUND, "Post not found");
                });
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
}
