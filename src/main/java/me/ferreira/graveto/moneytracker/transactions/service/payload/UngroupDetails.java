package me.ferreira.graveto.moneytracker.transactions.service.payload;

public record UngroupDetails(
    int removed,
    int alreadyUngrouped
) {
}
