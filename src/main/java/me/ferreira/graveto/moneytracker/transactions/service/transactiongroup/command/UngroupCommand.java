package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command;

import java.util.Set;
import java.util.UUID;

public record UngroupCommand(
    UUID userSid,
    UUID accountSid,
    Set<UUID> transactionSids
) {
}
