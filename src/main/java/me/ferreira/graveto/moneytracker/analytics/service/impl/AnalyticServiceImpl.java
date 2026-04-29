package me.ferreira.graveto.moneytracker.analytics.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateMonthlyAggregateCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AnalyticServiceImpl implements AnalyticService {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public CashFlowResult generateCashFlowReport(final CashFlowCommand command) {

        accountService
                .fetchAccount(new FetchAccountCommand(command.userSid(), command.accountSid()))
                .validateUserPermission(command.userSid(), MembershipRole::canRequestCashFlowReport, "request cash flow report");

        final GenerateMonthlyAggregateCommand aggregateCommand = new GenerateMonthlyAggregateCommand(
                command.year(),
                command.accountSid()
        );

        final List<MonthlyAggregateProjection> projections = transactionService.generateMonthlyAggregates(aggregateCommand);

        return null;
    }

}