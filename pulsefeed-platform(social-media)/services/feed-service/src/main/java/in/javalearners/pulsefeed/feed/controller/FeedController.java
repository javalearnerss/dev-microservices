package in.javalearners.pulsefeed.feed.controller;

import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing feed operations.
 * Post Service business calls are protected by {@code ResilientPostService};
 * User Service calls are direct. Post Service simulator controls are exposed by
 * the Post Service itself.
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
     * Calls User Service directly to get the following list and uses
     * {@code ResilientPostService} to retrieve posts.
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
     * Gets follower count directly from User Service.
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

}
