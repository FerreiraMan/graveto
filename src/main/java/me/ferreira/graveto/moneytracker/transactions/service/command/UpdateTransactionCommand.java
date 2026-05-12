package me.ferreira.graveto.moneytracker.transactions.service.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record UpdateTransactionCommand(
    UUID userSid,
    UUID transactionSid,
    TransactionType transactionType,
    UUID categorySid,
    BigDecimal amount,
    String description,
    LocalDateTime occurredAt
) {
}
