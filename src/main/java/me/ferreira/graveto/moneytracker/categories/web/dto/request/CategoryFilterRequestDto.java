package me.ferreira.graveto.moneytracker.categories.web.dto.request;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record CategoryFilterRequestDto(
    String displayName,
    UUID accountSid,
    UUID parentSid,
    TransactionType type
) {
}
