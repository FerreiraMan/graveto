package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Frequency;

public record CreateRecurringTransferRequestDto(
    @NotNull(message = "Source account identification is required.")
    UUID sourceAccountSid,

    @NotNull(message = "Destination account identification is required.")
    UUID destinationAccountSid,

    String description,

    @NotNull(message = "Amount is required.")
    @Positive(message = "Amount must be a positive value.")
    BigDecimal amount,

    @NotNull(message = "Frequency definition is required.")
    Frequency frequency,

    @Max(value = 31)
    @Min(value = 1)
    Integer dayOfMonth,

    @Max(value = 7)
    @Min(value = 1)
    Integer dayOfWeek,

    @NotNull(message = "Please specify if transactions must occur on business days.")
    Boolean adjustToBusinessDay,

    @FutureOrPresent
    LocalDate startDate,

    @FutureOrPresent
    LocalDate endDate
) {
}
