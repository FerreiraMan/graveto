package me.ferreira.graveto.moneytracker.transactions.service.command;

import java.util.UUID;

public record DeleteTransactionCommand(
    UUID userSid,
    UUID transactionSid
) {
}
