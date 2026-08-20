package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountWithInvalidOpeningBalanceException;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;

class MonthlyAggregateProjectionHelper {

  static MonthlyAggregateProjection resolveOpeningBalanceProjection(
      final UUID accountSid, final List<MonthlyAggregateProjection> projections) {

    return projections.stream()
        .filter(p -> TransactionType.OPENING_BALANCE.equals(p.getType()))
        .reduce((t1, t2) -> {
          throw new AccountWithInvalidOpeningBalanceException(accountSid, "DUPLICATE");
        })
        .orElseThrow(() -> new AccountWithInvalidOpeningBalanceException(accountSid, "NONEXISTENT"));
  }

  static TreeSet<Integer> resolveYearsWithCashFlows(final List<MonthlyAggregateProjection> projections) {

    return projections.stream()
        .filter(p -> TransactionType.INCOME.equals(p.getType()) || TransactionType.EXPENSE.equals(p.getType()))
        .map(MonthlyAggregateProjection::getYear)
        .collect(Collectors.toCollection(TreeSet::new));
  }

}
