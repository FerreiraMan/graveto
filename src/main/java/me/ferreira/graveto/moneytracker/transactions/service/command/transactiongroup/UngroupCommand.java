package me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup;

import java.util.Set;
import java.util.UUID;

public record UngroupCommand(
    UUID userSid,
    UUID accountSid,
    Set<UUID> transactionSids
) {
}
