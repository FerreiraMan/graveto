package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.util.UUID;

public record MembershipResponseDto(
    UUID sid,
    String email,
    String role
) {
}
