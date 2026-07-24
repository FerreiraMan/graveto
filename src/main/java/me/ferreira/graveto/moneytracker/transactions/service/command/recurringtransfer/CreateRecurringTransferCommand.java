package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Frequency;

public record CreateRecurringTransferCommand(
    UUID userSid,
    UUID sourceAccountSid,
    UUID destinationAccountSid,
    String description,
    BigDecimal amount,
    Frequency frequency,
    Integer dayOfMonth,
    Integer dayOfWeek,
    Boolean adjustToBusinessDay,
    LocalDate startDate,
    LocalDate endDate
) {
}
