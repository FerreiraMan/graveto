package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record FindAllRecurringTransfersCommand(
    UUID userSid,
    UUID accountSid,
    UUID destinationAccountSid,
    RecurringOperationStatus status
) {
}
