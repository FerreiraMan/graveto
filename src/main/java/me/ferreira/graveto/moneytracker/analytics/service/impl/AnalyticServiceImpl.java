package me.ferreira.graveto.moneytracker.analytics.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import me.ferreira.graveto.moneytracker.categories.service.command.FetchAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
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

    accountService
        .fetchAccountEntity(command.accountSid())
        .validateUserPermission(command.userSid(), MembershipRole::canRequestReport, "request cash flow report");

    final GenerateMonthlyAggregateCommand aggregateCommand = new GenerateMonthlyAggregateCommand(
        command.year(),
        command.accountSid()
    );

    final List<MonthlyAggregateProjection> projections = transactionService.generateMonthlyAggregates(aggregateCommand);

    return mapToCashFlowResult(command.year(), projections);
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
        categoryService.fetchAllCategories(new FetchAllCategoriesCommand(command.userSid(), command.accountSid()));

    return mapToCategorySpendingResult(command.year(), projections, accountAvailableCategories);
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