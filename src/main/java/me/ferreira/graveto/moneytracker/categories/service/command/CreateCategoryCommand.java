package me.ferreira.graveto.moneytracker.categories.service.command;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record CreateCategoryCommand(
    UUID userSid,
    String name,
    UUID parentSid,
    TransactionType transactionType
) {
}
