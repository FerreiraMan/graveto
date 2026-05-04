package me.ferreira.graveto.moneytracker.analytics.service.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CategorySpendingResult(
        int year,
        List<CategoryAggregate> categories
) {
    public record CategoryAggregate(
            UUID categorySid,
            String categoryName,
            BigDecimal yearlyTotal,
            Map<Integer, BigDecimal> monthlyTotals,
            List<CategoryAggregate> childCategories
    ) {}
}
