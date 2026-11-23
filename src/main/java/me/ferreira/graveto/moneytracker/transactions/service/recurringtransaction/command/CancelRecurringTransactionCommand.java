package me.ferreira.graveto.moneytracker.transactions.service.recurringtransaction.command;

import java.util.UUID;

public record CancelRecurringTransactionCommand(
    UUID userSid,
    UUID sid
) {
}
