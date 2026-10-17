package in.javalearners.pulsefeed.feed.service;

import feign.FeignException;
import in.javalearners.pulsefeed.feed.client.PostServiceClient;
import in.javalearners.pulsefeed.feed.model.Post;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Applies the Post Service resilience policies to business operations.
 *
 * <p>Policy values come from {@code resilience4j.*.instances.post-service} in
 * {@code application.yml}. Failure-simulator management endpoints belong to
 * Post Service and are called directly by clients; Feed Service does not proxy
 * those controls. Retry handles Feign failures and invokes its fallback only
 * after the configured attempts are exhausted.</p>
 *
 * <p>When the downstream service remains unavailable after retries, operations
 * fail with HTTP 503 rather than returning an empty collection or zero that
 * could be mistaken for valid data. Downstream 4xx responses retain their
 * status.</p>
 */
@Service
public class ResilientPostService {

    private static final Logger log = LoggerFactory.getLogger(ResilientPostService.class);

    private final PostServiceClient postServiceClient;

    /** A final downstream 5xx after Retry has exhausted its attempts. */
    public static class PostServiceUnavailableException extends ResponseStatusException {
        public PostServiceUnavailableException(String reason, Throwable cause) {
            super(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
        }
    }

    /**
     * Creates the resilient business-operation wrapper.
     *
     * @param postServiceClient raw Feign client for Post Service
     */
    public ResilientPostService(PostServiceClient postServiceClient) {
        this.postServiceClient = postServiceClient;
    }

    /**
     * Retrieves posts written by the specified authors.
     *
     * @param authorIds author identifiers
     * @return matching posts, newest first
     */
    @Retry(name = "post-service", fallbackMethod = "findByAuthorIdsFallback")
    @CircuitBreaker(name = "post-service")
    @Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
    public List<Post> findByAuthorIds(List<String> authorIds) {
        return postServiceClient.findByAuthorIds(authorIds);
    }

    /**
     * Retrieves a post by identifier.
     *
     * @param postId post identifier
     * @return the matching post
     */
    @Retry(name = "post-service", fallbackMethod = "getPostByIdFallback")
    @CircuitBreaker(name = "post-service")
    @Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
    public Post getPostById(String postId) {
        return postServiceClient.getPostById(postId);
    }

    /**
     * Retrieves posts by their identifiers.
     *
     * @param ids comma-separated post identifiers
     * @return matching posts
     */
    @Retry(name = "post-service", fallbackMethod = "getPostsByIdsFallback")
    @CircuitBreaker(name = "post-service")
    @Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
    public List<Post> getPostsByIds(String ids) {
        return postServiceClient.getPostsByIds(ids);
    }

    /**
     * Retrieves the number of posts written by an author.
     *
     * @param authorId author identifier
     * @return number of matching posts
     */
    @Retry(name = "post-service", fallbackMethod = "getPostCountByAuthorFallback")
    @CircuitBreaker(name = "post-service")
    @Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
    public Long getPostCountByAuthor(String authorId) {
        return postServiceClient.getPostCountByAuthor(authorId);
    }

    /**
     * Retrieves all posts written by one author.
     *
     * @param authorId author identifier
     * @return matching posts, newest first
     */
    @Retry(name = "post-service", fallbackMethod = "getPostsByAuthorFallback")
    @CircuitBreaker(name = "post-service")
    @Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
    public List<Post> getPostsByAuthor(String authorId) {
        return postServiceClient.getPostsByAuthor(authorId);
    }

    private List<Post> findByAuthorIdsFallback(List<String> authorIds, Throwable cause) {
        throw translateFailure("retrieve posts by author", cause);
    }

    private Post getPostByIdFallback(String postId, Throwable cause) {
        throw translateFailure("retrieve post", cause);
    }

    private List<Post> getPostsByIdsFallback(String ids, Throwable cause) {
        throw translateFailure("retrieve posts by ID", cause);
    }

    private Long getPostCountByAuthorFallback(String authorId, Throwable cause) {
        throw translateFailure("count posts by author", cause);
    }

    private List<Post> getPostsByAuthorFallback(String authorId, Throwable cause) {
        throw translateFailure("retrieve author's posts", cause);
    }

    private RuntimeException translateFailure(String operation, Throwable cause) {
        if (cause instanceof FeignException feignException
                && feignException.status() >= 400
                && feignException.status() < 500) {
            log.info("Post Service returned client error during {}: status={}", operation, feignException.status());
            return new ResponseStatusException(
                    HttpStatusCode.valueOf(feignException.status()),
                    "Post Service rejected the request",
                    cause);
        }

        log.warn("Post Service unavailable during {}", operation, cause);
        return new PostServiceUnavailableException(
                "Post Service is temporarily unavailable",
                cause);
    }
}
