package me.ferreira.graveto.moneytracker.transactions.service.transfer.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTransferCommand(
    UUID userSid,
    UUID correlationId,
    BigDecimal amount,
    String description,
    LocalDateTime occurredAt
) {
}
