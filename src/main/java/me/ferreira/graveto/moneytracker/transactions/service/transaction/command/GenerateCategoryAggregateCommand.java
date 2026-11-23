package me.ferreira.graveto.moneytracker.transactions.service.transaction.command;

import java.util.UUID;

public record GenerateCategoryAggregateCommand(
    int year,
    UUID accountSid
) {
}
