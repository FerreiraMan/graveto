package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.TreeMap;

class AccountBalanceHistory {

  private final TreeMap<Integer, BigDecimal> balanceHistoryAtEndOfYear = new TreeMap<>();

  Optional<BigDecimal> closingBalanceBefore(final int year) {

    final Integer priorYear = balanceHistoryAtEndOfYear.floorKey(year - 1);

    if (priorYear == null) {
      return Optional.empty();
    }
    return Optional.of(balanceHistoryAtEndOfYear.get(priorYear));
  }

  void recordClosingBalance(final int year, final BigDecimal balance) {
    balanceHistoryAtEndOfYear.put(year, balance);
  }

}
