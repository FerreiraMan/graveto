package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup.payload;

public record UngroupDetails(
    int removed,
    int alreadyUngrouped
) {
}
