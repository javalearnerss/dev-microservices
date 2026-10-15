package in.javalearners.pulsefeed.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point of the feed service, which builds a user's feed from the
 * user service (who they follow) and the post service (what those users posted).
 */
@SpringBootApplication
@EnableFeignClients
public class FeedApplication {

    /**
     * Starts the feed service.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FeedApplication.class, args);
    }
}
