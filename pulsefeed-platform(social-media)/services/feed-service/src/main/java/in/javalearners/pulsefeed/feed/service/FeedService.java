package in.javalearners.pulsefeed.feed.service;

import java.util.List;

import feign.FeignException;
import in.javalearners.pulsefeed.feed.client.PostServiceClient;
import in.javalearners.pulsefeed.feed.client.UserServiceClient;
import in.javalearners.pulsefeed.feed.model.Post;
import in.javalearners.pulsefeed.feed.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
     * @param userServiceClient client used to look up followed users
     * @param postServiceClient client used to fetch posts
     */
    public FeedService(UserServiceClient userServiceClient, PostServiceClient postServiceClient) {
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
            throw new ResponseStatusException(NOT_FOUND, "User not found: " + userId);
        }
    }
}
