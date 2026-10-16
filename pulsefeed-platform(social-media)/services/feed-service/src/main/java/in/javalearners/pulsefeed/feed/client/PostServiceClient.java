package in.javalearners.pulsefeed.feed.client;

import java.util.List;

import in.javalearners.pulsefeed.feed.model.Post;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for calling the post service over HTTP.
 * All methods trigger the Post Service failure simulator for testing resilience patterns.
 */
@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostServiceClient {

    /**
     * Retrieves the posts written by any of the given authors.
     * Triggers failure simulator in Post Service.
     *
     * @param authorIds identifiers of the authors whose posts are requested
     * @return posts by the given authors, newest first
     */
    @GetMapping("/api/posts")
    List<Post> findByAuthorIds(@RequestParam("authorIds") List<String> authorIds);

    /**
     * Retrieves multiple posts by IDs with failure simulation.
     *
     * @param ids comma-separated list of post identifiers
     * @return list of posts
     */
    @GetMapping("/api/posts/batch")
    List<Post> getPostsByIds(@RequestParam("ids") String ids);

    /**
     * Gets post count by author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return post count
     */
    @GetMapping("/api/posts/author/{authorId}/count")
    Long getPostCountByAuthor(@PathVariable("authorId") String authorId);

    /**
     * Gets all posts by a specific author with failure simulation.
     *
     * @param authorId identifier of the author
     * @return list of posts by this author
     */
    @GetMapping("/api/posts/author/{authorId}/posts")
    List<Post> getPostsByAuthor(@PathVariable("authorId") String authorId);

    // ==================== Failure Simulator Control Methods ====================

    /**
     * Starts slowness simulation in Post Service.
     *
     * @param delayMs delay in milliseconds
     * @return confirmation message
     */
    @PostMapping("/api/management/failure-simulator/slowness/start")
    ResponseEntity<String> startPostSlowness(@RequestParam(value = "delayMs", defaultValue = "500") long delayMs);

    /**
     * Stops slowness simulation in Post Service.
     *
     * @return confirmation message
     */
    @PostMapping("/api/management/failure-simulator/slowness/stop")
    ResponseEntity<String> stopPostSlowness();

    /**
     * Starts failure simulation in Post Service.
     *
     * @param failureCount number of failures to simulate
     * @return confirmation message
     */
    @PostMapping("/api/management/failure-simulator/failure/start")
    ResponseEntity<String> startPostFailure(@RequestParam(value = "count", defaultValue = "5") int failureCount);

    /**
     * Stops failure simulation in Post Service.
     *
     * @return confirmation message
     */
    @PostMapping("/api/management/failure-simulator/failure/stop")
    ResponseEntity<String> stopPostFailure();

    /**
     * Gets the status of Post Service failure simulator.
     *
     * @return status string
     */
    @GetMapping("/api/management/failure-simulator/status")
    ResponseEntity<String> getPostSimulatorStatus();

    /**
     * Resets Post Service failure simulator.
     *
     * @return confirmation message
     */
    @PostMapping("/api/management/failure-simulator/reset")
    ResponseEntity<String> resetPostSimulator();
}
