package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransaction;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record RecurringTransactionFilterRequestDto(
    RecurringOperationStatus status,
    UUID accountSid
) {
}
