package me.ferreira.graveto.moneytracker.analytics.web;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;
import me.ferreira.graveto.moneytracker.analytics.web.dto.CashFlowReportResponseDto;
import me.ferreira.graveto.moneytracker.analytics.web.dto.CategorySpendingReportResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  public static final String ACCOUNT_SID_PATH = "/{accountSid}";
  public static final String CASH_FLOW = "/cash-flow";
  public static final String CATEGORY_SPENDING = "/category-spending";

  private final AnalyticService analyticService;

  @GetMapping(path = ACCOUNT_SID_PATH + CASH_FLOW, produces = "application/json")
  public ResponseEntity<CashFlowReportResponseDto> fetchCashFlowReport(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID accountSid,
      @RequestParam(required = false) final Integer year) {

    final int targetYear = year != null ? year : Year.now().getValue();

    final CashFlowCommand command = new CashFlowCommand(userSid, accountSid, targetYear);

    final CashFlowResult cashFlowResult = analyticService.generateCashFlowReport(command);

    final List<CashFlowReportResponseDto.MonthlyCashFlowDto> mappedMonthlyFlows =
        cashFlowResult.monthlyCashFlow().stream()
            .map(m -> new CashFlowReportResponseDto.MonthlyCashFlowDto(
                m.month(),
                m.income(),
                m.expense(),
                m.netFlow()
            ))
            .toList();

    final CashFlowReportResponseDto response = new CashFlowReportResponseDto(
        cashFlowResult.year(),
        cashFlowResult.yearlyIncome(),
        cashFlowResult.yearlyExpense(),
        cashFlowResult.yearlyNetFlow(),
        mappedMonthlyFlows
    );

    return ResponseEntity.ok().body(response);
  }

  @GetMapping(path = ACCOUNT_SID_PATH + CATEGORY_SPENDING, produces = "application/json")
  public ResponseEntity<CategorySpendingReportResponseDto> fetchCategorySpendingReport(
      @AuthenticationPrincipal final UUID userSid,
      @PathVariable final UUID accountSid,
      @RequestParam(required = false) final Integer year) {

    final int targetYear = year != null ? year : Year.now().getValue();

    final CategorySpendingCommand command = new CategorySpendingCommand(userSid, accountSid, targetYear);

    final CategorySpendingResult categorySpendingResult = analyticService.generateCategorySpendingReport(command);

    final List<CategorySpendingReportResponseDto.CategoryAggregateResponseDto> mappedCategoryAggr =
        categorySpendingResult.categories().stream()
            .map(this::mapCategoryAggregate)
            .toList();

    final CategorySpendingReportResponseDto response = new CategorySpendingReportResponseDto(
        categorySpendingResult.year(),
        mappedCategoryAggr
    );

    return ResponseEntity.ok().body(response);
  }

  private CategorySpendingReportResponseDto.CategoryAggregateResponseDto mapCategoryAggregate(
      final CategorySpendingResult.CategoryAggregate aggregate) {

    final List<CategorySpendingReportResponseDto.CategoryAggregateResponseDto> mappedChildren =
        aggregate.childCategories() != null ? aggregate.childCategories().stream()
                                              .map(this::mapCategoryAggregate).toList() : List.of();

    return new CategorySpendingReportResponseDto.CategoryAggregateResponseDto(
        aggregate.categorySid(),
        aggregate.categoryName(),
        aggregate.yearlyTotal(),
        aggregate.monthlyTotals(),
        mappedChildren
    );
  }

}
