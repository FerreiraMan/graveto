package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateCategoryAggregateCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateMonthlyAggregateCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AnalyticServiceImpl implements AnalyticService {

  private final AccountService accountService;
  private final TransactionService transactionService;
  private final CategoryService categoryService;

  @Override
  @Transactional(readOnly = true)
  public CashFlowResult generateCashFlowReport(final CashFlowCommand command) {

    if (Year.now().getValue() < command.year()) {
      throw new IllegalArgumentException("Requested year must be present or past occurrence.");
    }

    accountService
        .fetchAccountEntity(command.accountSid())
        .validateUserPermission(command.userSid(), MembershipRole::canRequestReport, "request cash flow report");

    final List<MonthlyAggregateProjection> projections =
        transactionService.generateMonthlyAggregates(new GenerateMonthlyAggregateCommand(command.accountSid()));

    final MonthlyAggregateProjection openingBalanceProjection =
        MonthlyAggregateProjectionHelper.resolveOpeningBalanceProjection(command.accountSid(), projections);

    return mapToCashFlowResult(command.year(), openingBalanceProjection.getTotalAmount(),
        openingBalanceProjection.getYear(), projections);
  }

  @Override
  @Transactional(readOnly = true)
  public CategorySpendingResult generateCategorySpendingReport(final CategorySpendingCommand command) {

    accountService
        .fetchAccountEntity(command.accountSid())
        .validateUserPermission(command.userSid(), MembershipRole::canRequestReport,
            "request category spending report");

    final GenerateCategoryAggregateCommand aggregateCommand = new GenerateCategoryAggregateCommand(
        command.year(),
        command.accountSid()
    );

    final List<CategoryAggregateProjection> projections =
        transactionService.generateCategoryAggregates(aggregateCommand);

    final List<Category> accountAvailableCategories =
        categoryService.fetchAllCategories(
            new FindAllCategoriesCommand(command.userSid(), null, command.accountSid(), null, null));

    return mapToCategorySpendingResult(command.year(), projections, accountAvailableCategories);
  }

  private CashFlowResult mapToCashFlowResult(final int year, final BigDecimal accountOpeningBalance,
                                             final int yearAccountWasCreated,
                                             final List<MonthlyAggregateProjection> projections) {

    if (year < yearAccountWasCreated) {
      throw new IllegalArgumentException("Account has no movements yet to report.");
    }

    final TreeSet<Integer> yearsWithCashFlows = MonthlyAggregateProjectionHelper.resolveYearsWithCashFlows(projections);

    if (yearsWithCashFlows.isEmpty()) {
      return CashFlowResult.empty(year, accountOpeningBalance);
    }

    final AccountBalanceHistory accountBalanceHistory = new AccountBalanceHistory();
    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(projections, year);

    yearsWithCashFlows.forEach(aggregateYear -> {

      final BigDecimal startingBalance =
          accountBalanceHistory.closingBalanceBefore(aggregateYear).orElse(accountOpeningBalance);

      final BigDecimal accountBalanceAtEndOfYear = startingBalance
          .add(accumulator.incomeForYear(aggregateYear))
          .add(accumulator.transfersInForYear(aggregateYear))
          .subtract(accumulator.expenseForYear(aggregateYear))
          .subtract(accumulator.transfersOutForYear(aggregateYear));

      accountBalanceHistory.recordClosingBalance(aggregateYear, accountBalanceAtEndOfYear);
    });

    BigDecimal yearlyIncome = BigDecimal.ZERO;
    BigDecimal yearlyExpense = BigDecimal.ZERO;
    BigDecimal yearlyTransfersIn = BigDecimal.ZERO;
    BigDecimal yearlyTransfersOut = BigDecimal.ZERO;
    final List<CashFlowResult.MonthlyCashFlow> monthlyCashFlows = new ArrayList<>(12);
    BigDecimal monthlyStartingBalance = accountBalanceHistory.closingBalanceBefore(year).orElse(accountOpeningBalance);

    for (int month = 1; month <= 12; month++) {

      final BigDecimal income = accumulator.incomeForMonth(month);
      final BigDecimal expense = accumulator.expenseForMonth(month);
      final BigDecimal transfersIn = accumulator.transfersInForMonth(month);
      final BigDecimal transfersOut = accumulator.transfersOutForMonth(month);
      final BigDecimal balanceAtEndOfMonth =
          monthlyStartingBalance.add(income).add(transfersIn).subtract(expense).subtract(transfersOut);
      final BigDecimal monthlyNetIncomeExpense = income.subtract(expense);

      monthlyStartingBalance = balanceAtEndOfMonth;

      yearlyIncome = yearlyIncome.add(income);
      yearlyExpense = yearlyExpense.add(expense);
      yearlyTransfersIn = yearlyTransfersIn.add(transfersIn);
      yearlyTransfersOut = yearlyTransfersOut.add(transfersOut);

      monthlyCashFlows.add(
          new CashFlowResult.MonthlyCashFlow(
              month, income, expense, transfersIn, transfersOut, monthlyNetIncomeExpense, balanceAtEndOfMonth)
      );
    }

    final BigDecimal yearlyNetIncomeExpense = yearlyIncome.subtract(yearlyExpense);

    return new CashFlowResult(
        yearsWithCashFlows,
        year,
        yearlyIncome,
        yearlyExpense,
        yearlyTransfersIn,
        yearlyTransfersOut,
        yearlyNetIncomeExpense,
        monthlyStartingBalance,
        monthlyCashFlows
    );
  }

  private CategorySpendingResult mapToCategorySpendingResult(final int year,
                                                             final List<CategoryAggregateProjection> projections,
                                                             final List<Category> allUserCategories) {

    final Map<UUID, CategoryNode> nodeMap = new HashMap<>();

    for (final Category c : allUserCategories) {
      nodeMap.put(c.getSid(), new CategoryNode(c.getSid(), c.getDisplayName()));
    }

    for (final Category c : allUserCategories) {
      if (c.getParent() != null && nodeMap.containsKey(c.getParent().getSid())) {
        final CategoryNode childNode = nodeMap.get(c.getSid());
        final CategoryNode parentNode = nodeMap.get(c.getParent().getSid());

        childNode.parent = parentNode;
        parentNode.children.put(childNode.categorySid, childNode);
      }
    }

    for (final CategoryAggregateProjection p : projections) {

      CategoryNode currentNode = nodeMap.get(p.getCategorySid());

      while (currentNode != null) {
        currentNode.addAmount(p.getMonth(), p.getTotalAmount());
        currentNode = currentNode.parent;
      }
    }

    final List<CategorySpendingResult.CategoryAggregate> finalAggregates = nodeMap.values().stream()
        .filter(n -> n.parent == null && n.yearlyTotal.compareTo(BigDecimal.ZERO) > 0)
        .map(this::toImmutableRecord)
        .toList();

    return new CategorySpendingResult(year, finalAggregates);
  }

  private CategorySpendingResult.CategoryAggregate toImmutableRecord(final CategoryNode node) {

    final Map<Integer, BigDecimal> paddedMonthlyTotals = new HashMap<>();

    for (int month = 1; month <= 12; month++) {
      paddedMonthlyTotals.put(month, node.monthlyTotals.getOrDefault(month, BigDecimal.ZERO));
    }

    final List<CategorySpendingResult.CategoryAggregate> immutableChildren = node.children.values().stream()
        .filter(child -> child.yearlyTotal.compareTo(BigDecimal.ZERO) > 0)
        .map(this::toImmutableRecord)
        .toList();

    return new CategorySpendingResult.CategoryAggregate(
        node.categorySid,
        node.categoryName,
        node.yearlyTotal,
        paddedMonthlyTotals,
        immutableChildren
    );
  }

  private static class CategoryNode {

    final UUID categorySid;
    final String categoryName;
    final Map<Integer, BigDecimal> monthlyTotals = new HashMap<>();
    final Map<UUID, CategoryNode> children = new HashMap<>();
    CategoryNode parent;
    BigDecimal yearlyTotal = BigDecimal.ZERO;

    public CategoryNode(final UUID sid, final String name) {
      this.categorySid = sid;
      this.categoryName = name;
    }

    public void addAmount(final int month, final BigDecimal amount) {
      this.yearlyTotal = this.yearlyTotal.add(amount);
      this.monthlyTotals.merge(month, amount, BigDecimal::add);
    }
  }

}