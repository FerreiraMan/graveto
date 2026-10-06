package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountWithInvalidOpeningBalanceException;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;

class MonthlyAggregateProjectionHelper {

  private static final String ERROR_MESSAGE =
      "Account with SID [%s] with invalid amount of opening balance transactions: [%s]";

  static MonthlyAggregateProjection resolveOpeningBalanceProjection(
      final UUID accountSid, final List<MonthlyAggregateProjection> projections) {

    return projections.stream()
        .filter(p -> TransactionType.OPENING_BALANCE.equals(p.getType()))
        .reduce((t1, t2) -> {
          throw new AccountWithInvalidOpeningBalanceException(ERROR_MESSAGE.formatted(accountSid, "DUPLICATE"));
        })
        .orElseThrow(
            () -> new AccountWithInvalidOpeningBalanceException(ERROR_MESSAGE.formatted(accountSid, "NONEXISTENT")));
  }

  static TreeSet<Integer> resolveYearsWithCashFlows(final List<MonthlyAggregateProjection> projections) {

    return projections.stream()
        .filter(p -> p.getType().isValidCashFlow())
        .map(MonthlyAggregateProjection::getYear)
        .collect(Collectors.toCollection(TreeSet::new));
  }

}
