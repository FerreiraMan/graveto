package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction;

import java.util.UUID;

public record CancelRecurringTransactionCommand(
    UUID userSid,
    UUID sid
) {
}
