package in.javalearners.pulsefeed.feed.service;

import feign.FeignException;
import in.javalearners.pulsefeed.feed.client.PostServiceClient;
import in.javalearners.pulsefeed.feed.client.UserServiceClient;
import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


/**
 * Builds a user's feed by combining data from the user and post services.
 */
@Service
public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final UserServiceClient userServiceClient;
    private final PostServiceClient postServiceClient;

    /**
     * Creates the service.
     *
     * @param userServiceClient client used to look up followed users (resilient decorator)
     * @param postServiceClient client used to fetch posts (resilient decorator)
     */
    public FeedService(UserServiceClient userServiceClient,
                       @Qualifier("resilientPostServiceClient") PostServiceClient postServiceClient) {
        this.userServiceClient = userServiceClient;
        this.postServiceClient = postServiceClient;
    }

    /**
     * Returns the feed for a user: the posts of everyone the user follows.
     *
     * @param userId identifier of the user whose feed is requested
     * @return the feed posts, or an empty list if the user follows nobody
     * @throws ResponseStatusException with 404 if the user does not exist
     */
    public List<Post> getFeed(String userId) {
        log.info("FeedService.getFeed - Processing feed request for userId: {}", userId);
        List<String> followedIds = findFollowedIds(userId);
        log.info("FeedService.getFeed - User {} follows {} user(s): {}", userId, followedIds.size(), followedIds);
        if (followedIds.isEmpty()) {
            log.info("FeedService.getFeed - User {} follows nobody, returning empty feed", userId);
            return List.of();
        }
        log.info("FeedService.getFeed - Calling post-service to fetch posts from authorIds={}", followedIds);
        List<Post> posts = postServiceClient.findByAuthorIds(followedIds);
        log.info("FeedService.getFeed - Post-service returned {} posts for userId {}", posts == null ? 0 : posts.size(), userId);
        return posts == null ? List.of() : posts;
    }

    /**
     * Looks up the identifiers of the users followed by the given user.
     * Calls User Service endpoint that triggers failure simulator.
     *
     * @param userId identifier of the follower
     * @return identifiers of the followed users
     * @throws ResponseStatusException with 404 if the user service reports the user as unknown
     */
    private List<String> findFollowedIds(String userId) {
        try {
            log.info("FeedService.findFollowedIds - Calling user-service to retrieve following list for userId: {}", userId);
            List<User> following = userServiceClient.findFollowing(userId);
            List<String> followedIds = following == null ? List.of() : following.stream().map(User::id).toList();
            log.info("FeedService.findFollowedIds - User-service returned {} followed users: {}", followedIds.size(), followedIds);
            return followedIds;
        } catch (FeignException.NotFound e) {
            log.error("FeedService.findFollowedIds - User-service returned 404 for userId: {}, user not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
    }

    /**
     * Gets follower count via User Service with resilience patterns.
     * Calls endpoint that triggers User Service failure simulator.
     *
     * @param userId identifier of the user
     * @return follower count
     */
    public Long getFollowerCount(String userId) {
        try {
            log.info("FeedService.getFollowerCount - Getting follower count for userId: {}", userId);
            Long count = userServiceClient.getFollowerCount(userId);
            log.info("FeedService.getFollowerCount - User {} has {} followers", userId, count);
            return count;
        } catch (Exception e) {
            log.error("FeedService.getFollowerCount - Error getting follower count: {}", e.getMessage());
            throw new RuntimeException("Failed to get follower count", e);
        }
    }

    /**
     * Gets multiple posts via Post Service with resilience patterns.
     * Calls endpoint that triggers Post Service failure simulator.
     *
     * @param postIds comma-separated list of post identifiers
     * @return list of posts
     */
    public List<Post> getPostsBatch(String postIds) {
        try {
            log.info("FeedService.getPostsBatch - Fetching batch of posts: {}", postIds);
            List<Post> posts = postServiceClient.getPostsByIds(postIds);
            log.info("FeedService.getPostsBatch - Retrieved {} posts", posts.size());
            return posts;
        } catch (Exception e) {
            log.error("FeedService.getPostsBatch - Error retrieving posts: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve posts batch", e);
        }
    }

    // ==================== Failure Simulator Control Methods ====================

    /**
     * Starts slowness simulation in Post Service.
     * This is used for testing the timeout and retry patterns.
     *
     * @param delayMs delay in milliseconds
     * @return confirmation message
     */
    public String startPostServiceSlowness(long delayMs) {
        log.info("FeedService - Starting Post Service slowness with delay: {} ms", delayMs);
        try {
            ResponseEntity<String> response = postServiceClient.startPostSlowness(delayMs);
            String result = response.getBody();
            log.info("FeedService - Post Service slowness started: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error starting Post Service slowness: {}", e.getMessage());
            throw new RuntimeException("Failed to start Post Service slowness", e);
        }
    }

    /**
     * Stops slowness simulation in Post Service.
     *
     * @return confirmation message
     */
    public String stopPostServiceSlowness() {
        log.info("FeedService - Stopping Post Service slowness");
        try {
            ResponseEntity<String> response = postServiceClient.stopPostSlowness();
            String result = response.getBody();
            log.info("FeedService - Post Service slowness stopped: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error stopping Post Service slowness: {}", e.getMessage());
            throw new RuntimeException("Failed to stop Post Service slowness", e);
        }
    }

    /**
     * Starts failure simulation in Post Service.
     * This is used for testing the circuit breaker and retry patterns.
     *
     * @param failureCount number of failures to simulate
     * @return confirmation message
     */
    public String startPostServiceFailure(int failureCount) {
        log.info("FeedService - Starting Post Service failure simulation with count: {}", failureCount);
        try {
            ResponseEntity<String> response = postServiceClient.startPostFailure(failureCount);
            String result = response.getBody();
            log.info("FeedService - Post Service failure simulation started: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error starting Post Service failure: {}", e.getMessage());
            throw new RuntimeException("Failed to start Post Service failure", e);
        }
    }

    /**
     * Stops failure simulation in Post Service.
     *
     * @return confirmation message
     */
    public String stopPostServiceFailure() {
        log.info("FeedService - Stopping Post Service failure simulation");
        try {
            ResponseEntity<String> response = postServiceClient.stopPostFailure();
            String result = response.getBody();
            log.info("FeedService - Post Service failure simulation stopped: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error stopping Post Service failure: {}", e.getMessage());
            throw new RuntimeException("Failed to stop Post Service failure", e);
        }
    }

    /**
     * Gets Post Service failure simulator status.
     *
     * @return status string
     */
    public String getPostServiceSimulatorStatus() {
        log.info("FeedService - Getting Post Service failure simulator status");
        try {
            ResponseEntity<String> response = postServiceClient.getPostSimulatorStatus();
            String result = response.getBody();
            log.info("FeedService - Post Service status: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error getting Post Service status: {}", e.getMessage());
            throw new RuntimeException("Failed to get Post Service status", e);
        }
    }

    /**
     * Resets Post Service failure simulator.
     *
     * @return confirmation message
     */
    public String resetPostServiceSimulator() {
        log.info("FeedService - Resetting Post Service failure simulator");
        try {
            ResponseEntity<String> response = postServiceClient.resetPostSimulator();
            String result = response.getBody();
            log.info("FeedService - Post Service simulator reset: {}", result);
            return result;
        } catch (Exception e) {
            log.error("FeedService - Error resetting Post Service simulator: {}", e.getMessage());
            throw new RuntimeException("Failed to reset Post Service simulator", e);
        }
    }
}
