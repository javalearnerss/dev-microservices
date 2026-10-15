package in.javalearners.pulsefeed.feed.client;

import java.util.List;

import in.javalearners.pulsefeed.feed.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for calling the user service over HTTP.
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    /**
     * Retrieves the users that the given user follows.
     *
     * @param userId identifier of the follower
     * @return users followed by the given user
     */
    @GetMapping("/api/users/{userId}/following")
    List<User> findFollowing(@PathVariable("userId") String userId);
}
