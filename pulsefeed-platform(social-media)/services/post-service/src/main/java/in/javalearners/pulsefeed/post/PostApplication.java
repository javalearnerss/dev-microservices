package in.javalearners.pulsefeed.post;

import in.javalearners.pulsefeed.post.dto.CreatePostRequest;
import in.javalearners.pulsefeed.post.service.PostService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point of the post service, which stores and serves users' posts.
 */
@SpringBootApplication
public class PostApplication {

    private static final String SAM_ID = "user-sam";
    private static final String JORDAN_ID = "user-jordan";
    private static final String ALEX_ID = "user-alex";

    /**
     * Starts the post service.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PostApplication.class, args);
    }

    /**
     * Seeds a few dummy posts, authored by users seeded in the user service,
     * so that a followed user's feed has content at startup.
     *
     * @param postService service used to store the posts
     * @return a runner that creates the dummy posts on startup
     */
    @Bean
    CommandLineRunner seedPosts(PostService postService) {
        return args -> {
            postService.create(new CreatePostRequest(
                    SAM_ID,
                    "Just finished a great book and would love some recommendations."
            ));
            postService.create(new CreatePostRequest(
                    SAM_ID,
                    "The weekend hike was worth every step. The view was incredible."
            ));
            postService.create(new CreatePostRequest(
                    JORDAN_ID,
                    "Sharing a few photos from my latest trip. What is your next destination?"
            ));
            postService.create(new CreatePostRequest(
                    JORDAN_ID,
                    "Trying a new recipe tonight. I will share the results later."
            ));
            postService.create(new CreatePostRequest(
                    ALEX_ID,
                    "Working on an exciting new project. Stay tuned for updates!"
            ));
            postService.create(new CreatePostRequest(
                    ALEX_ID,
                    "Just discovered an amazing coffee shop downtown. Highly recommend it!"
            ));
        };
    }
}
