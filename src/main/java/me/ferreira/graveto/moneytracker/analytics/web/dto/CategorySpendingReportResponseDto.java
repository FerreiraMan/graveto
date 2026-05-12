package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CategorySpendingReportResponseDto(
    int year,
    List<CategoryAggregateResponseDto> categories
) {
  public record CategoryAggregateResponseDto(
      UUID categorySid,
      String categoryName,
      BigDecimal yearlyTotal,
      Map<Integer, BigDecimal> monthlyTotals,
      List<CategoryAggregateResponseDto> childCategories
  ) {
  }
}
