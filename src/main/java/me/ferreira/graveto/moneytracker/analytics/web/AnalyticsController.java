package me.ferreira.graveto.moneytracker.analytics.web;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;
import me.ferreira.graveto.moneytracker.analytics.web.dto.CashFlowReportResponseDTO;
import me.ferreira.graveto.moneytracker.analytics.web.dto.CategorySpendingReportResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    public static final String ACCOUNT_SID_PATH = "/{accountSid}";
    public static final String CASH_FLOW = "/cash-flow";
    public static final String CATEGORY_SPENDING = "/category-spending";

    private final AnalyticService analyticService;

    @GetMapping(path = ACCOUNT_SID_PATH + CASH_FLOW, produces = "application/json")
    public ResponseEntity<CashFlowReportResponseDTO> fetchCashFlowReport(
            @AuthenticationPrincipal final UUID userSid,
            @PathVariable final UUID accountSid,
            @RequestParam(required = false) final Integer year) {

        final int targetYear = year != null ? year : Year.now().getValue();

        final CashFlowCommand command = new CashFlowCommand(userSid, accountSid, targetYear);

        final CashFlowResult cashFlowResult = analyticService.generateCashFlowReport(command);

        final List<CashFlowReportResponseDTO.MonthlyCashFlowDTO> mappedMonthlyFlows = cashFlowResult.monthlyCashFlow().stream()
                .map(m -> new CashFlowReportResponseDTO.MonthlyCashFlowDTO(
                        m.month(),
                        m.income(),
                        m.expense(),
                        m.netFlow()
                ))
                .toList();

        final CashFlowReportResponseDTO response = new CashFlowReportResponseDTO(
                cashFlowResult.year(),
                cashFlowResult.yearlyIncome(),
                cashFlowResult.yearlyExpense(),
                cashFlowResult.yearlyNetFlow(),
                mappedMonthlyFlows
        );

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(path = ACCOUNT_SID_PATH + CATEGORY_SPENDING, produces = "application/json")
    public ResponseEntity<CategorySpendingReportResponseDTO> fetchCategorySpendingReport(
            @AuthenticationPrincipal final UUID userSid,
            @PathVariable final UUID accountSid,
            @RequestParam(required = false) final Integer year) {

        final int targetYear = year != null ? year : Year.now().getValue();

        final CategorySpendingCommand command = new CategorySpendingCommand(userSid, accountSid, targetYear);

        final CategorySpendingResult categorySpendingResult = analyticService.generateCategorySpendingReport(command);

        final List<CategorySpendingReportResponseDTO.CategoryAggregateResponseDTO> mappedCategoryAggr =
                categorySpendingResult.categories().stream()
                    .map(this::mapCategoryAggregate)
                    .toList();

        final CategorySpendingReportResponseDTO response = new CategorySpendingReportResponseDTO(
                categorySpendingResult.year(),
                mappedCategoryAggr
        );

        return ResponseEntity.ok().body(response);
    }

    private CategorySpendingReportResponseDTO.CategoryAggregateResponseDTO mapCategoryAggregate(
            final CategorySpendingResult.CategoryAggregate aggregate) {

        final List<CategorySpendingReportResponseDTO.CategoryAggregateResponseDTO> mappedChildren =
                aggregate.childCategories() != null
                        ? aggregate.childCategories().stream().map(this::mapCategoryAggregate).toList()
                        : List.of();

        return new CategorySpendingReportResponseDTO.CategoryAggregateResponseDTO(
                aggregate.categorySid(),
                aggregate.categoryName(),
                aggregate.yearlyTotal(),
                aggregate.monthlyTotals(),
                mappedChildren
        );
    }

}
