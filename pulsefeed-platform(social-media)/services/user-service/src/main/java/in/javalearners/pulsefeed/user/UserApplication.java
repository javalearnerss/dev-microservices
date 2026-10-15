package in.javalearners.pulsefeed.user;

import in.javalearners.pulsefeed.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point of the user service, which manages users and who follows whom.
 */
@SpringBootApplication
public class UserApplication {

    /** Identifier of the seeded user Alex Morgan. */
    static final String ALEX_ID = "user-alex";
    /** Identifier of the seeded user Sam Rivera. */
    static final String SAM_ID = "user-sam";
    /** Identifier of the seeded user Jordan Lee. */
    static final String JORDAN_ID = "user-jordan";

    /**
     * Starts the user service.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    /**
     * Seeds dummy users with stable identifiers and makes Alex follow Sam and Jordan.
     *
     * @param userService service used to store users and follow relationships
     * @return a runner that creates the dummy data on startup
     */
    @Bean
    CommandLineRunner seedUsers(UserService userService) {
        return args -> {
            userService.seed(ALEX_ID, "Alex Morgan", "alex@example.com");
            userService.seed(SAM_ID, "Sam Rivera", "sam@example.com");
            userService.seed(JORDAN_ID, "Jordan Lee", "jordan@example.com");

            // Alex follows Sam and Jordan
            userService.follow(ALEX_ID, SAM_ID);
            userService.follow(ALEX_ID, JORDAN_ID);

            // Sam follows Alex and Jordan
            userService.follow(SAM_ID, ALEX_ID);
            userService.follow(SAM_ID, JORDAN_ID);

            // Jordan follows Alex and Sam
            userService.follow(JORDAN_ID, ALEX_ID);
            userService.follow(JORDAN_ID, SAM_ID);
        };
    }
}
