package in.javalearners.pulsefeed.feed.service;

import feign.FeignException;
import in.javalearners.pulsefeed.feed.client.UserServiceClient;
import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private final ResilientPostService resilientPostService;

    /**
     * Creates the service with a direct User Service client and a resilient
     * wrapper for Post Service business operations.
     *
     * @param userServiceClient client used to look up following and follower counts
     * @param resilientPostService wrapper used for resilient Post Service business calls
     */
    public FeedService(UserServiceClient userServiceClient,
                       ResilientPostService resilientPostService) {
        this.userServiceClient = userServiceClient;
        this.resilientPostService = resilientPostService;
    }

    /**
     * Returns the feed for a user: the posts of everyone the user follows.
     *
     * @param userId identifier of the user whose feed is requested
     * @return the feed posts, or an empty list if the user follows nobody
     * @throws ResponseStatusException with 404 if the user does not exist
     */
    public List<Post> getFeed(String userId) {
        log.info("Retrieving feed for userId: {}", userId);
        List<String> followedIds = findFollowedIds(userId);
        if (followedIds.isEmpty()) {
            log.debug("No followed users found for userId: {}", userId);
            return List.of();
        }
        List<Post> posts = resilientPostService.findByAuthorIds(followedIds);
        return posts == null ? List.of() : posts;
    }

    /**
     * Looks up the identifiers of the users followed by the given user.
     * Calls User Service directly. User Service has no failure simulator or
     * resilience wrapper in the current implementation.
     *
     * @param userId identifier of the follower
     * @return identifiers of the followed users
     * @throws ResponseStatusException with 404 if the user service reports the user as unknown
     */
    private List<String> findFollowedIds(String userId) {
        try {
            List<User> following = userServiceClient.findFollowing(userId);
            return following == null ? List.of() : following.stream().map(User::id).toList();
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
    }

    /**
     * Retrieves the follower count directly from User Service.
     *
     * @param userId identifier of the user
     * @return follower count
     */
    public Long getFollowerCount(String userId) {
        log.debug("Retrieving follower count for userId: {}", userId);
        return userServiceClient.getFollowerCount(userId);
    }

    /**
     * Retrieves a batch of posts through the resilient Post Service wrapper.
     *
     * @param postIds comma-separated list of post identifiers
     * @return list of posts
     */
    public List<Post> getPostsBatch(String postIds) {
        log.debug("Retrieving batch of posts");
        List<Post> posts = resilientPostService.getPostsByIds(postIds);
        return posts == null ? List.of() : posts;
    }

}
