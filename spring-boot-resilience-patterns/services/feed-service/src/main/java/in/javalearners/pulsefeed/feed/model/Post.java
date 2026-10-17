package in.javalearners.pulsefeed.feed.model;

import java.time.Instant;

/**
 * A post as returned by the post service.
 *
 * @param id        unique identifier of the post
 * @param authorId  identifier of the user who wrote the post
 * @param content   text content of the post
 * @param createdAt time at which the post was created
 */
public record Post(String id, String authorId, String content, Instant createdAt) {
}
