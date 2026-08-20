package me.ferreira.graveto.moneytracker.analytics.service.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record CashFlowResult(
    Set<Integer> yearsWithCashFlows,
    int year,
    BigDecimal yearlyIncome,
    BigDecimal yearlyExpense,
    BigDecimal yearlyNetFlow,
    BigDecimal balanceAtEndOfYear,
    List<MonthlyCashFlow> monthlyCashFlow
) {
  public record MonthlyCashFlow(
      int month,
      BigDecimal income,
      BigDecimal expense,
      BigDecimal netFlow,
      BigDecimal balanceAtEndOfMonth
  ) {
  }
}
