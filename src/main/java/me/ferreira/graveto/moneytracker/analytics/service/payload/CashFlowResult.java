package me.ferreira.graveto.moneytracker.analytics.service.payload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record CashFlowResult(
    Set<Integer> yearsWithCashFlows,
    int year,
    BigDecimal yearlyIncome,
    BigDecimal yearlyExpense,
    BigDecimal yearlyTransfersIn,
    BigDecimal yearlyTransfersOut,
    BigDecimal yearlyNetIncomeExpense,
    BigDecimal balanceAtEndOfYear,
    List<MonthlyCashFlow> monthlyCashFlow
) {
  public record MonthlyCashFlow(
      int month,
      BigDecimal income,
      BigDecimal expense,
      BigDecimal transfersIn,
      BigDecimal transfersOut,
      BigDecimal monthlyNetIncomeExpense,
      BigDecimal balanceAtEndOfMonth
  ) {
  }

  public static CashFlowResult empty(final int year, final BigDecimal accountOpeningBalance) {

    final List<CashFlowResult.MonthlyCashFlow> emptyMonthlyPayload = new ArrayList<>();

    for (int month = 1; month <= 12; month++) {
      emptyMonthlyPayload.add(
          new CashFlowResult.MonthlyCashFlow(month,
              BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
              accountOpeningBalance));
    }

    return new CashFlowResult(
        Set.of(),
        year,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        accountOpeningBalance,
        emptyMonthlyPayload
    );
  }
}
