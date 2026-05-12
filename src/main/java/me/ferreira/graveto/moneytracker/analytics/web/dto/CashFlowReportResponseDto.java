package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowReportResponseDto(
    int year,
    BigDecimal yearlyIncome,
    BigDecimal yearlyExpense,
    BigDecimal yearlyNetFlow,
    List<MonthlyCashFlowDto> monthlyCashFlow
) {
  public record MonthlyCashFlowDto(
      int month,
      BigDecimal income,
      BigDecimal expense,
      BigDecimal netFlow
  ) {
  }
}
