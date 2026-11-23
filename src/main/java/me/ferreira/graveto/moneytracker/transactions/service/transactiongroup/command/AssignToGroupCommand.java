package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.command;

import java.util.Set;
import java.util.UUID;

public record AssignToGroupCommand(
    UUID userSid,
    UUID accountSid,
    UUID groupSid,
    String groupName,
    Set<UUID> transactionSids,
    boolean replaceExistingGroup
) {
}
