package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload;

import java.util.List;
import java.util.UUID;

public record GroupConflict(
    UUID groupSid,
    String groupName,
    List<UUID> transactionSids
) {
}
