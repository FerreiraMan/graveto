package me.ferreira.graveto.identity.web.response;

import java.util.UUID;

public record RegisterResponseDTO(
    UUID sid,
    String email
) {}
