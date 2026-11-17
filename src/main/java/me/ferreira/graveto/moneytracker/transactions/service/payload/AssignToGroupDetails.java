package me.ferreira.graveto.moneytracker.transactions.service.payload;

import java.util.UUID;

public record AssignToGroupDetails(
    UUID groupSid,
    String groupName,
    Long totalTransactions,
    int added,
    int moved,
    int alreadyInGroup
) {
}
