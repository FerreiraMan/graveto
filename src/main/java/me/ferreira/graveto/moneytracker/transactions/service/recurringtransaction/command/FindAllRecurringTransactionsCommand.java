package me.ferreira.graveto.moneytracker.transactions.service.recurringtransaction.command;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record FindAllRecurringTransactionsCommand(
    UUID userSid,
    UUID accountSid,
    RecurringOperationStatus status
) {
}
