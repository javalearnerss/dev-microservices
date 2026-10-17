package in.javalearners.pulsefeed.post.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a post.
 *
 * @param authorId identifier of the user writing the post
 * @param content  text content of the post
 */
public record CreatePostRequest(
        @NotBlank String authorId,
        @NotBlank String content
) {
}
