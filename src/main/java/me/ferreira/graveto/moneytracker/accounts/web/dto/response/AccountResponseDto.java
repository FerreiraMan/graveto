package me.ferreira.graveto.moneytracker.accounts.web.dto.response;

import java.util.UUID;

public record AccountResponseDto(
    UUID sid,
    String status
) {
}
