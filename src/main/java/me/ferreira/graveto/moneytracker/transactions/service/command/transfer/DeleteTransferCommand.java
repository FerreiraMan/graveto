package me.ferreira.graveto.moneytracker.transactions.service.command.transfer;

import java.util.UUID;

public record DeleteTransferCommand(
    UUID userSid,
    UUID correlationId
) {
}
