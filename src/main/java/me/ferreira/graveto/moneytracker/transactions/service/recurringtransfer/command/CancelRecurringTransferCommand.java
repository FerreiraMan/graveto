package me.ferreira.graveto.moneytracker.transactions.service.recurringtransfer.command;

import java.util.UUID;

public record CancelRecurringTransferCommand(
    UUID userSid,
    UUID sid
) {
}
