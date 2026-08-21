package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;

class MonthlyAggregateAccumulator {

  private final Map<Integer, BigDecimal> yearlyIncome = new HashMap<>();
  private final Map<Integer, BigDecimal> yearlyExpense = new HashMap<>();
  private final Map<Integer, BigDecimal> yearlyTransfersIn = new HashMap<>();
  private final Map<Integer, BigDecimal> yearlyTransfersOut = new HashMap<>();
  private final Map<Integer, BigDecimal> monthlyIncome = new HashMap<>();
  private final Map<Integer, BigDecimal> monthlyExpense = new HashMap<>();
  private final Map<Integer, BigDecimal> monthlyTransfersIn = new HashMap<>();
  private final Map<Integer, BigDecimal> monthlyTransfersOut = new HashMap<>();

  MonthlyAggregateAccumulator(final List<MonthlyAggregateProjection> projections, final int targetYear) {
    for (final MonthlyAggregateProjection p : projections) {

      if (p.getType() == TransactionType.INCOME) {

        yearlyIncome.merge(p.getYear(), p.getTotalAmount(), BigDecimal::add);
        if (p.getYear() == targetYear) {
          monthlyIncome.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
        }

      } else if (p.getType() == TransactionType.EXPENSE) {

        yearlyExpense.merge(p.getYear(), p.getTotalAmount(), BigDecimal::add);
        if (p.getYear() == targetYear) {
          monthlyExpense.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
        }

      } else if (p.getType() == TransactionType.TRANSFER_IN) {

        yearlyTransfersIn.merge(p.getYear(), p.getTotalAmount(), BigDecimal::add);
        if (p.getYear() == targetYear) {
          monthlyTransfersIn.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
        }

      } else if (p.getType() == TransactionType.TRANSFER_OUT) {

        yearlyTransfersOut.merge(p.getYear(), p.getTotalAmount(), BigDecimal::add);
        if (p.getYear() == targetYear) {
          monthlyTransfersOut.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
        }

      }
    }
  }

  BigDecimal incomeForYear(final int year) {
    return yearlyIncome.getOrDefault(year, BigDecimal.ZERO);
  }

  BigDecimal expenseForYear(final int year) {
    return yearlyExpense.getOrDefault(year, BigDecimal.ZERO);
  }

  BigDecimal incomeForMonth(final int month) {
    return monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
  }

  BigDecimal expenseForMonth(final int month) {
    return monthlyExpense.getOrDefault(month, BigDecimal.ZERO);
  }

  BigDecimal transfersInForYear(final int year) {
    return yearlyTransfersIn.getOrDefault(year, BigDecimal.ZERO);
  }

  BigDecimal transfersOutForYear(final int year) {
    return yearlyTransfersOut.getOrDefault(year, BigDecimal.ZERO);
  }

  BigDecimal transfersInForMonth(final int month) {
    return monthlyTransfersIn.getOrDefault(month, BigDecimal.ZERO);
  }

  BigDecimal transfersOutForMonth(final int month) {
    return monthlyTransfersOut.getOrDefault(month, BigDecimal.ZERO);
  }

}
