package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;

public record CashFlowReportResponseDto(
    Set<Integer> yearsWithCashFlows,
    int year,
    BigDecimal yearlyIncome,
    BigDecimal yearlyExpense,
    BigDecimal yearlyNetFlow,
    BigDecimal balanceAtEndOfYear,
    List<MonthlyCashFlowDto> monthlyCashFlow
) {
  public record MonthlyCashFlowDto(
      int month,
      BigDecimal income,
      BigDecimal expense,
      BigDecimal netFlow,
      BigDecimal balanceAtEndOfMonth
  ) {
  }

  public static CashFlowReportResponseDto from(final CashFlowResult cashFlowResult) {

    return new CashFlowReportResponseDto(
        cashFlowResult.yearsWithCashFlows(),
        cashFlowResult.year(),
        cashFlowResult.yearlyIncome(),
        cashFlowResult.yearlyExpense(),
        cashFlowResult.yearlyNetFlow(),
        cashFlowResult.balanceAtEndOfYear(),
        from(cashFlowResult.monthlyCashFlow())
    );
  }

  private static List<MonthlyCashFlowDto> from(final List<CashFlowResult.MonthlyCashFlow> monthlyCashFlows) {

    return monthlyCashFlows.stream()
        .map(m -> new CashFlowReportResponseDto.MonthlyCashFlowDto(
            m.month(),
            m.income(),
            m.expense(),
            m.netFlow(),
            m.balanceAtEndOfMonth()
        ))
        .toList();
  }

}
