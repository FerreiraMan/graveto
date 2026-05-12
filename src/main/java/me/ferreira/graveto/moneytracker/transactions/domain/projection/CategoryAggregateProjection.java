package me.ferreira.graveto.moneytracker.transactions.domain.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface CategoryAggregateProjection {
  int getMonth();

  UUID getCategorySid();

  BigDecimal getTotalAmount();
}
