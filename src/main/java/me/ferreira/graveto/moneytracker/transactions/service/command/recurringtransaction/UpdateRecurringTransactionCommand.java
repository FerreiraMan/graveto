package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record UpdateRecurringTransactionCommand(
    UUID userSid,
    UUID sid,
    String description,
    BigDecimal amount,
    Frequency frequency,
    Integer dayOfMonth,
    Integer dayOfWeek,
    Boolean adjustToBusinessDay,
    RecurringOperationStatus status,
    LocalDate nextExecutionDate,
    LocalDate endDate
) {
}
