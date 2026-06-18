package me.ferreira.graveto.moneytracker.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateMonthlyAggregateCommand;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  void shouldThrowIfAccountIsNotFoundDuringCashFlowGeneration() {
    // Arrange
    final UUID accountSid = UUID.randomUUID();

    when(accountService.fetchAccountEntity(any())).thenThrow(new AccountNotFoundException(accountSid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.generateCashFlowReport(Mockito.mock(CashFlowCommand.class));
    }).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account with SID [" + accountSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToRequestCashFlowGeneration() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final CashFlowCommand command = mock(CashFlowCommand.class);

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.generateCashFlowReport(command);
    }).isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to request cash flow report for this account.");
  }

  @Test
  void shouldMapMonthlyAggregateToCashFlowResult() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);
    final GenerateMonthlyAggregateCommand monthlyAggregateCommand =
        new GenerateMonthlyAggregateCommand(year, account.getSid());

    final List<MonthlyAggregateProjection> projections = List.of(
        // January (Month 1): $5000 Income, $2000 Expense
        new MockProjection(1, TransactionType.INCOME, new BigDecimal("5000.00")),
        new MockProjection(1, TransactionType.INCOME, new BigDecimal("100.00")),
        new MockProjection(1, TransactionType.EXPENSE, new BigDecimal("2000.00")),

        // March (Month 3): $500 Expense
        new MockProjection(3, TransactionType.INCOME, new BigDecimal("50.00")),
        new MockProjection(3, TransactionType.EXPENSE, new BigDecimal("500.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(monthlyAggregateCommand)).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    assertThat(cashFlowResult.year()).isEqualTo(year);
    assertThat(cashFlowResult.yearlyIncome()).isEqualByComparingTo("5150.00");
    assertThat(cashFlowResult.yearlyExpense()).isEqualByComparingTo("2500.00");
    assertThat(cashFlowResult.yearlyNetFlow()).isEqualByComparingTo("2650.00");

    assertThat(cashFlowResult.monthlyCashFlow()).hasSize(12);

    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.month()).isEqualTo(1);
    assertThat(january.income()).isEqualByComparingTo("5100.00");
    assertThat(january.expense()).isEqualByComparingTo("2000.00");
    assertThat(january.netFlow()).isEqualByComparingTo("3100.00");

    final CashFlowResult.MonthlyCashFlow february = cashFlowResult.monthlyCashFlow().get(1);
    assertThat(february.month()).isEqualTo(2);
    assertThat(february.income()).isEqualByComparingTo("0.00");
    assertThat(february.expense()).isEqualByComparingTo("0.00");
    assertThat(february.netFlow()).isEqualByComparingTo("0.00");

    final CashFlowResult.MonthlyCashFlow march = cashFlowResult.monthlyCashFlow().get(2);
    assertThat(march.month()).isEqualTo(3);
    assertThat(march.income()).isEqualByComparingTo("50.00");
    assertThat(march.expense()).isEqualByComparingTo("500.00");
    assertThat(march.netFlow()).isEqualByComparingTo("-450.00");
  }

  @Test
  void shouldDiscardTransfersDuringCashFlowResult() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final int year = 2026;

    final CashFlowCommand command = new CashFlowCommand(userSid, account.getSid(), year);
    final GenerateMonthlyAggregateCommand monthlyAggregateCommand =
        new GenerateMonthlyAggregateCommand(year, account.getSid());

    final List<MonthlyAggregateProjection> projections = List.of(
        new MockProjection(1, TransactionType.TRANSFER_IN, new BigDecimal("5000.00")),
        new MockProjection(3, TransactionType.TRANSFER_OUT, new BigDecimal("50.00"))
    );

    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(transactionService.generateMonthlyAggregates(monthlyAggregateCommand)).thenReturn(projections);

    // Act
    final CashFlowResult cashFlowResult = service.generateCashFlowReport(command);

    // Assert
    assertThat(cashFlowResult.year()).isEqualTo(year);
    assertThat(cashFlowResult.yearlyIncome()).isEqualByComparingTo("0.00");
    assertThat(cashFlowResult.yearlyExpense()).isEqualByComparingTo("0.00");
    assertThat(cashFlowResult.yearlyNetFlow()).isEqualByComparingTo("0.00");

    assertThat(cashFlowResult.monthlyCashFlow()).hasSize(12);

    final CashFlowResult.MonthlyCashFlow january = cashFlowResult.monthlyCashFlow().get(0);
    assertThat(january.month()).isEqualTo(1);
    assertThat(january.income()).isEqualByComparingTo("0.00");
    assertThat(january.expense()).isEqualByComparingTo("0.00");
    assertThat(january.netFlow()).isEqualByComparingTo("0.00");

    final CashFlowResult.MonthlyCashFlow march = cashFlowResult.monthlyCashFlow().get(2);
    assertThat(march.month()).isEqualTo(3);
    assertThat(march.income()).isEqualByComparingTo("0.00");
    assertThat(march.expense()).isEqualByComparingTo("0.00");
    assertThat(march.netFlow()).isEqualByComparingTo("0.00");
  }

  private record MockProjection(int month, TransactionType type,
                                BigDecimal amount) implements MonthlyAggregateProjection {
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
