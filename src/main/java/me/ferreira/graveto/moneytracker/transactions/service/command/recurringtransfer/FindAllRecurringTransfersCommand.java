package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record FindAllRecurringTransfersCommand(
    UUID userSid,
    RecurringOperationStatus status,
    UUID sourceAccountSid,
    UUID destinationAccountSid
) {
}
