package me.ferreira.graveto.moneytracker.analytics.web;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.analytics.web.dto.CashFlowReportResponseDTO;
import org.springframework.http.ResponseEntity;
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

    private final AnalyticService analyticService;

    @GetMapping(path = ACCOUNT_SID_PATH + CASH_FLOW, produces = "application/json")
    public ResponseEntity<CashFlowReportResponseDTO> fetchCashFlowReport(
            @RequestHeader("X-User-Sid") final UUID userSid,
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
}
