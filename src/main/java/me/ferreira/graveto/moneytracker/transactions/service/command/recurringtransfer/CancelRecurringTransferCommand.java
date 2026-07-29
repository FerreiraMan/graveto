package me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer;

import java.util.UUID;

public record CancelRecurringTransferCommand(
    UUID userSid,
    UUID sid
) {
}
