package me.ferreira.graveto.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDto(
    @NotBlank(message = "Email cannot be empty.")
    @Email(message = "Must be a well-formed email address")
    String email
) {
}
