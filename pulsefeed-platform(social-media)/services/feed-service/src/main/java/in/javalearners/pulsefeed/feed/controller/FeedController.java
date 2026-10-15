package in.javalearners.pulsefeed.feed.controller;

import java.util.List;

import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the feed endpoints.
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
}
