package me.ferreira.graveto.moneytracker.transactions.service.command;

import java.util.UUID;

public record GenerateMonthlyAggregateCommand(
    int year,
    UUID accountSid
) {
}
