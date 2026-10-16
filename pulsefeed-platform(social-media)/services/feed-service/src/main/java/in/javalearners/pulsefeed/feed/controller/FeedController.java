package in.javalearners.pulsefeed.feed.controller;

import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing the feed endpoints.
 * All endpoints call User/Post services which trigger failure simulators.
 * Feed Service applies resilience patterns (timeout, retry, circuit breaker, bulkhead) to these calls.
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private static final Logger log = LoggerFactory.getLogger(FeedController.class);

    private final FeedService feedService;

    /**
     * Creates the controller.
     *
     * @param feedService service that builds feeds
     */
    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Returns the feed of the given user.
     * Calls User Service (with resilience) to get following list.
     * Calls Post Service (with resilience) to get posts.
     *
     * @param userId identifier of the user whose feed is requested
     * @return posts written by the users that the given user follows
     */
    @GetMapping("/{userId}")
    public List<Post> getFeed(@PathVariable String userId) {
        log.info("GET /api/feed/{} - Request received, building feed for userId: {}", userId, userId);
        List<Post> feed = feedService.getFeed(userId);
        log.info("GET /api/feed/{} - Feed built successfully, returning {} posts", userId, feed.size());
        return feed;
    }

    /**
     * Gets follower count from User Service with resilience patterns.
     *
     * @param userId identifier of the user
     * @return follower count
     */
    @GetMapping("/user/{userId}/followers/count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable String userId) {
        log.info("GET /api/feed/user/{}/followers/count - Request received", userId);
        Long count = feedService.getFollowerCount(userId);
        log.info("GET /api/feed/user/{}/followers/count - Count: {}", userId, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Gets multiple posts from Post Service with resilience patterns.
     *
     * @param ids comma-separated list of post identifiers
     * @return list of posts
     */
    @GetMapping("/posts/batch")
    public List<Post> getPostsBatch(@RequestParam String ids) {
        log.info("GET /api/feed/posts/batch - Request received for posts: {}", ids);
        List<Post> posts = feedService.getPostsBatch(ids);
        log.info("GET /api/feed/posts/batch - Retrieved {} posts", posts.size());
        return posts;
    }

    // ==================== Failure Simulator Control Endpoints ====================
    // These endpoints allow Feed Service to control the failure simulators in User and Post services

    /**
     * Starts slowness simulation in Post Service.
     * Used for testing timeout and retry patterns.
     *
     * @param delayMs delay in milliseconds
     * @return confirmation message
     */
    @PostMapping("/management/post-service/slowness/start")
    public ResponseEntity<String> startPostServiceSlowness(@org.springframework.web.bind.annotation.RequestParam(value = "delayMs", defaultValue = "500") long delayMs) {
        log.info("POST /api/feed/management/post-service/slowness/start - Delay: {} ms", delayMs);
        String result = feedService.startPostServiceSlowness(delayMs);
        log.info("POST /api/feed/management/post-service/slowness/start - Result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Stops slowness simulation in Post Service.
     *
     * @return confirmation message
     */
    @PostMapping("/management/post-service/slowness/stop")
    public ResponseEntity<String> stopPostServiceSlowness() {
        log.info("POST /api/feed/management/post-service/slowness/stop");
        String result = feedService.stopPostServiceSlowness();
        log.info("POST /api/feed/management/post-service/slowness/stop - Result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Starts failure simulation in Post Service.
     * Used for testing circuit breaker and retry patterns.
     *
     * @param failureCount number of failures to simulate
     * @return confirmation message
     */
    @PostMapping("/management/post-service/failure/start")
    public ResponseEntity<String> startPostServiceFailure(@RequestParam(value = "count", defaultValue = "5") int failureCount) {
        log.info("POST /api/feed/management/post-service/failure/start - Count: {}", failureCount);
        String result = feedService.startPostServiceFailure(failureCount);
        log.info("POST /api/feed/management/post-service/failure/start - Result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Stops failure simulation in Post Service.
     *
     * @return confirmation message
     */
    @PostMapping("/management/post-service/failure/stop")
    public ResponseEntity<String> stopPostServiceFailure() {
        log.info("POST /api/feed/management/post-service/failure/stop");
        String result = feedService.stopPostServiceFailure();
        log.info("POST /api/feed/management/post-service/failure/stop - Result: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Gets Post Service failure simulator status.
     *
     * @return status string
     */
    @GetMapping("/management/post-service/status")
    public ResponseEntity<String> getPostServiceSimulatorStatus() {
        log.info("GET /api/feed/management/post-service/status");
        String status = feedService.getPostServiceSimulatorStatus();
        log.info("GET /api/feed/management/post-service/status - Status: {}", status);
        return ResponseEntity.ok(status);
    }

    /**
     * Resets Post Service failure simulator.
     *
     * @return confirmation message
     */
    @PostMapping("/management/post-service/reset")
    public ResponseEntity<String> resetPostServiceSimulator() {
        log.info("POST /api/feed/management/post-service/reset");
        String result = feedService.resetPostServiceSimulator();
        log.info("POST /api/feed/management/post-service/reset - Result: {}", result);
        return ResponseEntity.ok(result);
    }
}
