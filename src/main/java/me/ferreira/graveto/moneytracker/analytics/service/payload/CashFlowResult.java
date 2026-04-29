package me.ferreira.graveto.moneytracker.analytics.service.payload;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowResult(
        int year,
        BigDecimal yearlyIncome,
        BigDecimal yearlyExpense,
        BigDecimal yearlyNetFlow,
        List<MonthlyCashFlow> monthlyCashFlow
) {
    public record MonthlyCashFlow(
            int month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal netFlow
    ) {}
}
