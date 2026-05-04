package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CategorySpendingReportResponseDTO(
        int year,
        List<CategoryAggregateResponseDTO> categories
) {
    public record CategoryAggregateResponseDTO(
            UUID categorySid,
            String categoryName,
            BigDecimal yearlyTotal,
            Map<Integer, BigDecimal> monthlyTotals,
            List<CategoryAggregateResponseDTO> childCategories
    ) {}
}
