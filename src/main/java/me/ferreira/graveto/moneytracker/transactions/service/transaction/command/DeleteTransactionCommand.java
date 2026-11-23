package me.ferreira.graveto.moneytracker.transactions.service.transaction.command;

import java.util.UUID;

public record DeleteTransactionCommand(
    UUID userSid,
    UUID transactionSid
) {
}
