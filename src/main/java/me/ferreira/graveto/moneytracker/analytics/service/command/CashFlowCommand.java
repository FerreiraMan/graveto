package me.ferreira.graveto.moneytracker.analytics.service.command;

import java.util.UUID;

public record CashFlowCommand(
    UUID userSid,
    UUID accountSid,
    int year
) {
}
