package in.javalearners.pulsefeed.feed.client;

import java.util.List;

import in.javalearners.pulsefeed.feed.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for calling the user service over HTTP.
 * All methods trigger the User Service failure simulator for testing resilience patterns.
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    /**
     * Retrieves the users that the given user follows.
     * Triggers failure simulator in User Service.
     *
     * @param userId identifier of the follower
     * @return users followed by the given user
     */
    @GetMapping("/api/users/{userId}/following")
    List<User> findFollowing(@PathVariable("userId") String userId);

    /**
     * Gets follower count with failure simulation.
     *
     * @param userId identifier of the user
     * @return follower count
     */
    @GetMapping("/api/users/{userId}/followers/count")
    Long getFollowerCount(@PathVariable("userId") String userId);
}
