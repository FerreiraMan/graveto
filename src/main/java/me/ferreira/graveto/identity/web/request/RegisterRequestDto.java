package me.ferreira.graveto.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank(message = "Email cannot be empty.")
    @Email(message = "Must be a well-formed email address")
    String email,

    @NotBlank(message = "Password cannot be empty.")
    @Size(min = 5, message = "Password does not comply with the defined minimum size.")
    String password
) {
}
