package me.ferreira.graveto.identity.web.response;

import java.util.UUID;

public record RegisterResponseDto(
    UUID sid,
    String email
) {
}
