package me.ferreira.graveto.moneytracker.transactions.domain.projection;

import java.math.BigDecimal;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public interface MonthlyAggregateProjection {
  int getMonth();

  TransactionType getType();

  BigDecimal getTotalAmount();
}
