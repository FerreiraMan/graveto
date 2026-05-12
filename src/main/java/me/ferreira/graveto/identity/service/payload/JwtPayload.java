package me.ferreira.graveto.identity.service.payload;

import java.util.UUID;

public record JwtPayload(
    UUID sid,
    String role
) {
}
