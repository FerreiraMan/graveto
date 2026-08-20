package me.ferreira.graveto.moneytracker.analytics.service.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountBalanceHistoryTest {

  @Test
  void shouldReturnEmptyWhenNoPriorYearIsRecorded() {
    final AccountBalanceHistory history = new AccountBalanceHistory();

    assertThat(history.closingBalanceBefore(2024)).isEmpty();
  }

  @Test
  void shouldReturnRecordedBalanceForImmediatelyPriorYear() {
    final AccountBalanceHistory history = new AccountBalanceHistory();
    history.recordClosingBalance(2023, new BigDecimal("1300.00"));

    assertThat(history.closingBalanceBefore(2024)).contains(new BigDecimal("1300.00"));
  }

  @Test
  void shouldWalkBackThroughGapYearsToFindTheNearestPriorBalance() {
    final AccountBalanceHistory history = new AccountBalanceHistory();
    history.recordClosingBalance(2020, new BigDecimal("1300.00"));
    // no entry for 2021

    assertThat(history.closingBalanceBefore(2022)).contains(new BigDecimal("1300.00"));
  }

  @Test
  void shouldNotReturnFutureOrSameYearBalance() {
    final AccountBalanceHistory history = new AccountBalanceHistory();
    history.recordClosingBalance(2024, new BigDecimal("1300.00"));

    assertThat(history.closingBalanceBefore(2024)).isEmpty();
  }

}
