package me.ferreira.graveto.moneytracker.transactions.service.transfer.command;

import java.util.UUID;

public record DeleteTransferCommand(
    UUID userSid,
    UUID correlationId
) {
}
