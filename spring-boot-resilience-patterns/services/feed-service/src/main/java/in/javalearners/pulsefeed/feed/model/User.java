package in.javalearners.pulsefeed.feed.model;

/**
 * A user as returned by the user service.
 *
 * @param id    unique identifier of the user
 * @param name  display name of the user
 * @param email email address of the user
 */
public record User(String id, String name, String email) {
}
