package me.ferreira.graveto.moneytracker.transactions.service.command;

import java.util.UUID;

public record GenerateCategoryAggregateCommand(
    int year,
    UUID accountSid
) {
}
