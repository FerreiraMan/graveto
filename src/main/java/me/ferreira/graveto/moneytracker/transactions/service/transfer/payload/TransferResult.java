package me.ferreira.graveto.moneytracker.transactions.service.transfer.payload;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;

public record TransferResult(
    Transaction expense,
    Transaction income
) {}
