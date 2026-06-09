package me.ferreira.graveto.identity.api;

import java.util.UUID;

public record UserResponseDto(
    UUID sid,
    String email
) {
}
