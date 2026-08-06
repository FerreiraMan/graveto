package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record FindAllCategoriesCommand(
    UUID userSid,
    String displayName,
    UUID accountSid,
    UUID parentSid,
    TransactionType type
) {
}
