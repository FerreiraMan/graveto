package me.ferreira.graveto.identity.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "Email cannot be empty.")
    String email,

    @NotBlank(message = "Password cannot be empty.")
    String password
) {}
