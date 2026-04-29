package me.ferreira.graveto.moneytracker.transactions.domain.projection;

import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

import java.math.BigDecimal;

public interface MonthlyAggregateProjection {
    int getMonth();
    TransactionType getType();
    BigDecimal getTotalAmount();
}
