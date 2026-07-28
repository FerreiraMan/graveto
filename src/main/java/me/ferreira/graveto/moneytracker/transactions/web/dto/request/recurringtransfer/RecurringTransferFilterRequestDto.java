package me.ferreira.graveto.moneytracker.transactions.web.dto.request.recurringtransfer;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record RecurringTransferFilterRequestDto(
    RecurringOperationStatus status,
    UUID sourceAccountSid,
    UUID destinationAccountSid
) {
}
