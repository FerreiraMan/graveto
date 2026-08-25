package me.ferreira.graveto.moneytracker.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;

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

  public static CategorySpendingReportResponseDto from(final CategorySpendingResult categorySpendingResult) {

    return new CategorySpendingReportResponseDto(
        categorySpendingResult.year(),
        from(categorySpendingResult.categories())
    );
  }

  private static List<CategoryAggregateResponseDto> from(
      final List<CategorySpendingResult.CategoryAggregate> categoryAggregates) {

    return categoryAggregates.stream()
        .map(CategorySpendingReportResponseDto::mapCategoryAggregate)
        .toList();
  }

  private static CategoryAggregateResponseDto mapCategoryAggregate(
      final CategorySpendingResult.CategoryAggregate aggregate) {

    final List<CategorySpendingReportResponseDto.CategoryAggregateResponseDto> mappedChildren =
        aggregate.childCategories() != null ? aggregate.childCategories().stream()
            .map(CategorySpendingReportResponseDto::mapCategoryAggregate).toList() : List.of();

    return new CategorySpendingReportResponseDto.CategoryAggregateResponseDto(
        aggregate.categorySid(),
        aggregate.categoryName(),
        aggregate.yearlyTotal(),
        aggregate.monthlyTotals(),
        mappedChildren
    );
  }

}
