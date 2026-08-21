package me.ferreira.graveto.moneytracker.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountWithInvalidOpeningBalanceException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.impl.AnalyticServiceImpl;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GenerateCashFlowReportServiceImplTest {

  @InjectMocks
  private AnalyticServiceImpl service;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionService transactionService;

  @Test
  void shouldThrowIfRequestedYearIsInTheFuture() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int futureYear = Year.now().getValue() + 1;
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), futureYear);

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requested year must be present or past occurrence.");
  }

  @Test
  void shouldThrowIfAccountIsNotFoundDuringCashFlowGeneration() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();
    final CashFlowCommand command = new CashFlowCommand(UUID.randomUUID(), accountSid, Year.now().getValue());

    when(accountService.fetchAccountEntity(any())).thenThrow(new AccountNotFoundException(accountSid));

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account with SID [" + accountSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToRequestCashFlowGeneration() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), Year.now().getValue());

    when(accountService.fetchAccountEntity(any())).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to request cash flow report for this account.");
  }

  @Test
  void shouldThrowIfAccountHasNoOpeningBalanceTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(List.of(
        new MockProjection(year, 1, TransactionType.INCOME, new BigDecimal("100.00"))
    ));

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(AccountWithInvalidOpeningBalanceException.class)
        .hasMessage(
            "Account with SID [" + account.getSid() + "] with invalid amount of opening balance transactions: "
                + "NONEXISTENT");
  }

  @Test
  void shouldThrowIfAccountHasDuplicateOpeningBalanceTransactions() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(List.of(
        new MockProjection(2024, 1, TransactionType.OPENING_BALANCE, new BigDecimal("500.00")),
        new MockProjection(2024, 1, TransactionType.OPENING_BALANCE, new BigDecimal("500.00"))
    ));

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(AccountWithInvalidOpeningBalanceException.class)
        .hasMessage(
            "Account with SID [" + account.getSid() + "] with invalid amount of opening balance transactions: "
                + "DUPLICATE");
  }

  @Test
  void shouldThrowIfRequestedYearIsBeforeAccountCreation() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), 2023);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(List.of(
        openingBalance(2024, "1000.00"),
        new MockProjection(2024, 1, TransactionType.INCOME, new BigDecimal("100.00"))
    ));

    // Act & Assert
    assertThatThrownBy(() -> service.generateCashFlowReport(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Account has no movements yet to report.");
  }

  @Test
  void shouldReturnEmptyReportWhenAccountHasNoIncomeOrExpenseActivityYet() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;
    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(List.of(
        openingBalance(year, "1000.00")
    ));

    // Act
    final CashFlowResult result = service.generateCashFlowReport(command);

    // Assert
    assertThat(result.year()).isEqualTo(year);
    assertThat(result.yearsWithCashFlows()).isEmpty();
    assertThat(result.yearlyIncome()).isEqualByComparingTo("0.00");
    assertThat(result.yearlyExpense()).isEqualByComparingTo("0.00");
    assertThat(result.yearlyNetIncomeExpense()).isEqualByComparingTo("0.00");
    assertThat(result.balanceAtEndOfYear()).isEqualByComparingTo("1000.00");

    assertThat(result.monthlyCashFlow()).hasSize(12);
    result.monthlyCashFlow().forEach(m -> {
      assertThat(m.income()).isEqualByComparingTo("0.00");
      assertThat(m.expense()).isEqualByComparingTo("0.00");
      assertThat(m.monthlyNetIncomeExpense()).isEqualByComparingTo("0.00");
      assertThat(m.balanceAtEndOfMonth()).isEqualByComparingTo("1000.00");
    });
  }

  @Test
  void shouldMapMonthlyAggregateToCashFlowResult() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    final List<MonthlyAggregateProjection> projections = List.of(
        openingBalance(year, "0.00"),

        // January (Month 1): $5000 Income, $2000 Expense
        new MockProjection(year, 1, TransactionType.INCOME, new BigDecimal("5000.00")),
        new MockProjection(year, 1, TransactionType.INCOME, new BigDecimal("100.00")),
        new MockProjection(year, 1, TransactionType.EXPENSE, new BigDecimal("2000.00")),

        // March (Month 3): $500 Expense
        new MockProjection(year, 3, TransactionType.INCOME, new BigDecimal("50.00")),
        new MockProjection(year, 3, TransactionType.EXPENSE, new BigDecimal("500.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    assertThat(cashFlowResult.year()).isEqualTo(year);
    assertThat(cashFlowResult.yearlyIncome()).isEqualByComparingTo("5150.00");
    assertThat(cashFlowResult.yearlyExpense()).isEqualByComparingTo("2500.00");
    assertThat(cashFlowResult.yearlyNetIncomeExpense()).isEqualByComparingTo("2650.00");
    assertThat(cashFlowResult.balanceAtEndOfYear()).isEqualByComparingTo("2650.00");

    assertThat(cashFlowResult.monthlyCashFlow()).hasSize(12);

    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.month()).isEqualTo(1);
    assertThat(january.income()).isEqualByComparingTo("5100.00");
    assertThat(january.expense()).isEqualByComparingTo("2000.00");
    assertThat(january.monthlyNetIncomeExpense()).isEqualByComparingTo("3100.00");
    assertThat(january.balanceAtEndOfMonth()).isEqualByComparingTo("3100.00");

    final CashFlowResult.MonthlyCashFlow february = cashFlowResult.monthlyCashFlow().get(1);
    assertThat(february.month()).isEqualTo(2);
    assertThat(february.income()).isEqualByComparingTo("0.00");
    assertThat(february.expense()).isEqualByComparingTo("0.00");
    assertThat(february.monthlyNetIncomeExpense()).isEqualByComparingTo("0.00");
    assertThat(february.balanceAtEndOfMonth()).isEqualByComparingTo("3100.00");

    final CashFlowResult.MonthlyCashFlow march = cashFlowResult.monthlyCashFlow().get(2);
    assertThat(march.month()).isEqualTo(3);
    assertThat(march.income()).isEqualByComparingTo("50.00");
    assertThat(march.expense()).isEqualByComparingTo("500.00");
    assertThat(march.monthlyNetIncomeExpense()).isEqualByComparingTo("-450.00");
    assertThat(march.balanceAtEndOfMonth()).isEqualByComparingTo("2650.00");

    final CashFlowResult.MonthlyCashFlow december = cashFlowResult.monthlyCashFlow().get(11);
    assertThat(december.balanceAtEndOfMonth()).isEqualByComparingTo("2650.00");
  }

  @Test
  void shouldIncludeTransfersInBalanceButNotInIncomeOrExpense() {
    // Arrange - transfers must move the balance (they are real money movements) but must
    // never be counted as income/expense/netIncomeExpense, which are reserved for true
    // earning/spending activity.
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    final List<MonthlyAggregateProjection> projections = List.of(
        openingBalance(year, "1000.00"),
        new MockProjection(year, 1, TransactionType.TRANSFER_IN, new BigDecimal("500.00")),
        new MockProjection(year, 3, TransactionType.TRANSFER_OUT, new BigDecimal("200.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    // Transfers must still register as "activity" for the year, otherwise a transfer-only
    // account would incorrectly be treated as having no cash flow at all.
    assertThat(cashFlowResult.yearsWithCashFlows()).containsExactly(year);

    // Income/expense/netIncomeExpense stay untouched by transfers.
    assertThat(cashFlowResult.yearlyIncome()).isEqualByComparingTo("0.00");
    assertThat(cashFlowResult.yearlyExpense()).isEqualByComparingTo("0.00");
    assertThat(cashFlowResult.yearlyNetIncomeExpense()).isEqualByComparingTo("0.00");

    // Transfers are surfaced separately.
    assertThat(cashFlowResult.yearlyTransfersIn()).isEqualByComparingTo("500.00");
    assertThat(cashFlowResult.yearlyTransfersOut()).isEqualByComparingTo("200.00");

    // Balance is 1000 (opening) + 500 (transfer in) - 200 (transfer out) = 1300.
    assertThat(cashFlowResult.balanceAtEndOfYear()).isEqualByComparingTo("1300.00");

    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.transfersIn()).isEqualByComparingTo("500.00");
    assertThat(january.transfersOut()).isEqualByComparingTo("0.00");
    assertThat(january.income()).isEqualByComparingTo("0.00");
    assertThat(january.expense()).isEqualByComparingTo("0.00");
    assertThat(january.monthlyNetIncomeExpense()).isEqualByComparingTo("0.00");
    assertThat(january.balanceAtEndOfMonth()).isEqualByComparingTo("1500.00");

    final CashFlowResult.MonthlyCashFlow march = cashFlowResult.monthlyCashFlow().get(2);
    assertThat(march.transfersIn()).isEqualByComparingTo("0.00");
    assertThat(march.transfersOut()).isEqualByComparingTo("200.00");
    assertThat(march.balanceAtEndOfMonth()).isEqualByComparingTo("1300.00");

    final CashFlowResult.MonthlyCashFlow december = cashFlowResult.monthlyCashFlow().get(11);
    assertThat(december.balanceAtEndOfMonth()).isEqualByComparingTo("1300.00");
  }

  @Test
  void shouldCarryTransferAffectedBalanceForwardThroughGapYear() {
    // Arrange - opening balance in 2023, a transfer-out in 2024 (no income/expense that
    // year), income in 2025. The 2024 transfer must still reduce the balance carried into
    // 2025, proving the balance-history walk-back picks up transfer-only years too.
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2025;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    final List<MonthlyAggregateProjection> projections = List.of(
        openingBalance(2023, "1000.00"),
        new MockProjection(2024, 6, TransactionType.TRANSFER_OUT, new BigDecimal("300.00")),
        new MockProjection(year, 1, TransactionType.INCOME, new BigDecimal("500.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    // 2024 closing balance = 1000 (opening) - 300 (transfer out) = 700.
    // January 2025 closing balance = 700 + 500 (income) = 1200.
    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.balanceAtEndOfMonth()).isEqualByComparingTo("1200.00");
    assertThat(cashFlowResult.balanceAtEndOfYear()).isEqualByComparingTo("1200.00");
  }

  @Test
  void shouldCarryBalanceForwardThroughGapYearWithNoActivity() {
    // Arrange - opening balance in 2023, income in 2024, nothing in 2025, income in 2026.
    // Requesting 2026 must carry the 2024 closing balance forward through the 2025 gap,
    // rather than resetting to the raw opening balance.
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    final List<MonthlyAggregateProjection> projections = List.of(
        openingBalance(2023, "1000.00"),
        new MockProjection(2024, 6, TransactionType.INCOME, new BigDecimal("300.00")),
        new MockProjection(year, 1, TransactionType.INCOME, new BigDecimal("500.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    // 2024 closing balance = 1000 (opening) + 300 (income) = 1300.
    // 2025 has no activity, so 2026 must start from 1300, not from the raw opening balance.
    // January 2026 closing balance = 1300 + 500 = 1800.
    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.balanceAtEndOfMonth()).isEqualByComparingTo("1800.00");
    assertThat(cashFlowResult.balanceAtEndOfYear()).isEqualByComparingTo("1800.00");
  }

  @Test
  void shouldReportFlatBalanceWhenRequestedYearItselfIsGapYear() {
    // Arrange - income in 2023, nothing in 2024, income in 2025. Requesting 2024 (the gap
    // year itself) must show a flat balance across all 12 months, held at the 2023 closing
    // balance.
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2024;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);

    final List<MonthlyAggregateProjection> projections = List.of(
        openingBalance(2023, "1000.00"),
        new MockProjection(2023, 6, TransactionType.INCOME, new BigDecimal("300.00")),
        new MockProjection(2025, 1, TransactionType.INCOME, new BigDecimal("700.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(any())).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert - 2023 closing balance = 1000 + 300 = 1300, flat through all of 2024.
    assertThat(cashFlowResult.balanceAtEndOfYear()).isEqualByComparingTo("1300.00");
    cashFlowResult.monthlyCashFlow().forEach(m -> {
      assertThat(m.income()).isEqualByComparingTo("0.00");
      assertThat(m.expense()).isEqualByComparingTo("0.00");
      assertThat(m.balanceAtEndOfMonth()).isEqualByComparingTo("1300.00");
    });
  }

  private static MockProjection openingBalance(final int year, final String amount) {
    return new MockProjection(year, 1, TransactionType.OPENING_BALANCE, new BigDecimal(amount));
  }

  private record MockProjection(int year, int month, TransactionType type,
                                BigDecimal amount) implements MonthlyAggregateProjection {
    @Override
    public int getYear() {
      return year;
    }

    @Override
    public int getMonth() {
      return month;
    }

    @Override
    public TransactionType getType() {
      return type;
    }

    @Override
    public BigDecimal getTotalAmount() {
      return amount;
    }
  }

}
