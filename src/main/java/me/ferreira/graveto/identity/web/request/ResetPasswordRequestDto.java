package me.ferreira.graveto.identity.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
    @NotBlank(message = "Token cannot be empty.")
    String token,

    @NotBlank(message = "Password cannot be empty.")
    @Size(min = 5, message = "Password does not comply with the defined minimum size.")
    String newPassword
) {
}
