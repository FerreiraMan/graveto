package me.ferreira.graveto.moneytracker.analytics.web;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.FullAccountResponseDTO;
import me.ferreira.graveto.moneytracker.accounts.web.dto.response.MembershipResponseDTO;
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
    public ResponseEntity<CashFlowReportResponseDTO> fetchAccount(
            @RequestHeader("X-User-Sid") final UUID userSid,
            @PathVariable final UUID accountSid,
            @RequestParam(required = false) final Integer year) {

        final int targetYear = year != null ? year : Year.now().getValue();

        final CashFlowCommand command = new CashFlowCommand(userSid, accountSid, targetYear);

        final CashFlowResult cashFlowResult = analyticService.generateCashFlowReport(command);


        return null;
    }
}
