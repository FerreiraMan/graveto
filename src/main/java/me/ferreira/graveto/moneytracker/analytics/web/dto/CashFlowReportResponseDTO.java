package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowReportResponseDTO(
    int year,
    BigDecimal yearlyIncome,
    BigDecimal yearlyExpense,
    BigDecimal yearlyNetFlow,
    List<MonthlyCashFlowDTO> monthlyCashFlow
) {
    public record MonthlyCashFlowDTO(
        int month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netFlow
    ) {}
}
