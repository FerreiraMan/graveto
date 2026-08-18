package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record RecurringTransferFilterRequestDto(
    @NotNull
    UUID accountSid,
    UUID destinationAccountSid,
    RecurringOperationStatus status
) {
}
