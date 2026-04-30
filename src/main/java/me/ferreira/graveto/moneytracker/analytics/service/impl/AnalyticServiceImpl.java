package me.ferreira.graveto.moneytracker.analytics.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateMonthlyAggregateCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@AllArgsConstructor
public class AnalyticServiceImpl implements AnalyticService {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @Override
    @Transactional(readOnly = true)
    public CashFlowResult generateCashFlowReport(final CashFlowCommand command) {

        accountService
                .fetchAccount(new FetchAccountCommand(command.userSid(), command.accountSid()))
                .validateUserPermission(command.userSid(), MembershipRole::canRequestCashFlowReport, "request cash flow report");

        final GenerateMonthlyAggregateCommand aggregateCommand = new GenerateMonthlyAggregateCommand(
                command.year(),
                command.accountSid()
        );

        final List<MonthlyAggregateProjection> projections = transactionService.generateMonthlyAggregates(aggregateCommand);

        return mapToCashFlowResult(command.year(), projections);
    }

    private CashFlowResult mapToCashFlowResult(final int year, final List<MonthlyAggregateProjection> projections) {

        final HashMap<Integer, BigDecimal> monthlyIncomeMap = new HashMap<>();
        final HashMap<Integer, BigDecimal> monthlyExpenseMap = new HashMap<>();

        for (final MonthlyAggregateProjection p : projections) {

            if (p.getType() == TransactionType.INCOME) {
                monthlyIncomeMap.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
            } else if (p.getType() == TransactionType.EXPENSE) {
                monthlyExpenseMap.merge(p.getMonth(), p.getTotalAmount(), BigDecimal::add);
            }
        }

        BigDecimal yearlyIncome = BigDecimal.ZERO;
        BigDecimal yearlyExpense = BigDecimal.ZERO;
        final List<CashFlowResult.MonthlyCashFlow> monthlyCashFlows = new ArrayList<>(12);

        for (int month = 1; month <= 12; month++) {

            final BigDecimal income = monthlyIncomeMap.getOrDefault(month, BigDecimal.ZERO);
            final BigDecimal expense = monthlyExpenseMap.getOrDefault(month, BigDecimal.ZERO);
            final BigDecimal netFlow = income.subtract(expense);

            yearlyIncome = yearlyIncome.add(income);
            yearlyExpense = yearlyExpense.add(expense);

            monthlyCashFlows.add(new CashFlowResult.MonthlyCashFlow(month, income, expense, netFlow));
        }

        final BigDecimal yearlyNetFlow = yearlyIncome.subtract(yearlyExpense);

        return new CashFlowResult(
                year,
                yearlyIncome,
                yearlyExpense,
                yearlyNetFlow,
                monthlyCashFlows
        );
    }

}