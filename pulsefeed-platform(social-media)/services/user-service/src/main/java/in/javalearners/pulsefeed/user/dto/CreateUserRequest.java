package in.javalearners.pulsefeed.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a user.
 *
 * @param name  display name of the user
 * @param email valid email address of the user
 */
public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
