package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record RecurringTransactionFilterRequestDto(
    @NotNull
    UUID accountSid,
    RecurringOperationStatus status
) {
}
