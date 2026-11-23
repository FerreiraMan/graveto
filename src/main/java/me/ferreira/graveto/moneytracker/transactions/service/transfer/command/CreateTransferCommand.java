package me.ferreira.graveto.moneytracker.transactions.service.transfer.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransferCommand(
    UUID userSid,
    UUID sourceAccountSid,
    UUID destinationAccountSid,
    BigDecimal amount,
    String description,
    LocalDateTime occurredAt
) {
}
