package me.ferreira.graveto.identity.web.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank
    String email,

    @NotBlank
    String password
) {}
