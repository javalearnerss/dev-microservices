package in.javalearners.pulsefeed.feed.client;

import java.util.List;

import in.javalearners.pulsefeed.feed.model.Post;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for calling the post service over HTTP.
 */
@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostServiceClient {

    /**
     * Retrieves the posts written by any of the given authors.
     *
     * @param authorIds identifiers of the authors whose posts are requested
     * @return posts by the given authors, newest first
     */
    @GetMapping("/api/posts")
    List<Post> findByAuthorIds(@RequestParam("authorIds") List<String> authorIds);
}
