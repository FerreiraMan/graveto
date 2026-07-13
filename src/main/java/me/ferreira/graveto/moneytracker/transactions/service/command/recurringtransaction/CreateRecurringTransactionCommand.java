package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public record CreateRecurringTransactionCommand(
    UUID userSid,
    UUID accountSid,
    UUID categorySid,
    String description,
    BigDecimal amount,
    TransactionType transactionType,
    Frequency frequency,
    Integer dayOfMonth,
    Integer dayOfWeek,
    Boolean adjustToBusinessDay,
    LocalDate startDate,
    LocalDate endDate
) {
}