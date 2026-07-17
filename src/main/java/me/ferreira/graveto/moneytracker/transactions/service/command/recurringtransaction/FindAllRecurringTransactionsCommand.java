package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;

public record FindAllRecurringTransactionsCommand(
    UUID userSid,
    RecurringOperationStatus status,
    UUID accountSid
) {
}
