package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record UpdateRecurringTransactionRequestDto(
    String description,

    @Positive(message = "Amount must be a positive value.")
    BigDecimal amount,

    Frequency frequency,

    @Max(value = 31)
    @Min(value = 1)
    Integer dayOfMonth,

    @Max(value = 7)
    @Min(value = 1)
    Integer dayOfWeek,

    Boolean adjustToBusinessDay,

    RecurringOperationStatus status,

    @Future
    LocalDate nextExecutionDate,

    @Future
    LocalDate endDate
) {
}
