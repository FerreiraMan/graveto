package me.ferreira.graveto.moneytracker.transactions.service.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateRecurringTransactionServiceImplTest {

  @InjectMocks
  private RecurringTransactionServiceImpl recurringTransactionService;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private RecurringTransactionRepository recurringTransactionRepository;

  @Test
  void shouldThrowWhenMonthlyFrequencyAndDayOfMonthIsNull() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.MONTHLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month needs to be provided when selecting monthly operation.");

    verify(recurringTransactionRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenWeeklyFrequencyAndDayOfWeekIsNull() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.WEEKLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");

    verify(recurringTransactionRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenBiWeeklyFrequencyAndDayOfWeekIsNull() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.BI_WEEKLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
  }

  @Test
  void shouldThrowWhenAnnuallyAndBothDayOfMonthAndStartDateAreNull() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.ANNUALLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month or an explicit start date needs to be provided when selecting annual operation.");
  }

  @Test
  void shouldThrowWhenEndDateIsBeforeStartDate() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.MONTHLY, 15, null, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 1));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("End date must be after start date.");
  }

  @Test
  void shouldThrowWhenCategoryTypeDoesNotMatchTransactionType() {
    // Arrange
    final CreateRecurringTransactionCommand command = buildCommand(
        Frequency.MONTHLY, 15, null, null, null);

    final Category category = new Category();
    category.setSid(command.categorySid());
    category.setTransactionType(TransactionType.INCOME);

    when(categoryService.fetchCategory(any())).thenReturn(category);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Category type [INCOME] does not match the requested transaction type [EXPENSE].");
  }

  @Test
  void shouldThrowWhenAccountIsNotActive() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = new Account();
    account.setSid(command.accountSid());
    account.setStatus(AccountStatus.CLOSED);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.createRecurringTransaction(command))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldCreateRecurringTransactionSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final LocalDate startDate = LocalDate.of(2026, 8, 15);
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, startDate, null);

    final Category category = new Category();
    category.setSid(command.categorySid());
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    final ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
    verify(recurringTransactionRepository).save(captor.capture());

    final RecurringTransaction saved = captor.getValue();
    assertThat(saved.getSid()).isNotNull();
    assertThat(saved.getAccount()).isEqualTo(account);
    assertThat(saved.getCategory()).isEqualTo(category);
    assertThat(saved.getUserSid()).isEqualTo(userSid);
    assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(saved.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(saved.getDayOfTheMonth()).isEqualTo(15);
    assertThat(saved.getAdjustToBusinessDay()).isTrue();
    assertThat(saved.getNextExecutionDate()).isEqualTo(startDate);
    assertThat(saved.getStartDate()).isEqualTo(startDate);
    assertThat(saved.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(saved.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(result).isEqualTo(saved);
  }

  @Test
  void shouldResolveStartDateWhenNotProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    assertThat(result.getStartDate()).isNotNull();
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getStartDate()).isEqualTo(result.getNextExecutionDate());
  }

  @Test
  void shouldResolveStartDateForWeeklyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.WEEKLY, null, 1, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(1);
  }

  @Test
  void shouldResolveStartDateForBiWeeklyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.BI_WEEKLY, null, 5, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(5);
  }

  @Test
  void shouldResolveStartDateForAnnuallyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.ANNUALLY, 20, null, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfMonth()).isEqualTo(20);
  }

  @Test
  void shouldResolveStartDateForDailyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransactionCommand command = buildCommandWithUser(
        userSid, Frequency.DAILY, null, null, null, null);

    final Category category = new Category();
    category.setTransactionType(TransactionType.EXPENSE);
    when(categoryService.fetchCategory(any())).thenReturn(category);

    final Account account = buildAccount(command.accountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransaction result = recurringTransactionService.createRecurringTransaction(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate()).isAfter(LocalDate.now());
  }

  private static CreateRecurringTransactionCommand buildCommand(final Frequency frequency,
                                                                final Integer dayOfMonth,
                                                                final Integer dayOfWeek,
                                                                final LocalDate startDate,
                                                                final LocalDate endDate) {
    return buildCommandWithUser(UUID.randomUUID(), frequency, dayOfMonth, dayOfWeek, startDate, endDate);
  }

  private static CreateRecurringTransactionCommand buildCommandWithUser(final UUID userSid,
                                                                        final Frequency frequency,
                                                                        final Integer dayOfMonth,
                                                                        final Integer dayOfWeek,
                                                                        final LocalDate startDate,
                                                                        final LocalDate endDate) {
    return new CreateRecurringTransactionCommand(
        userSid,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Home Insurance",
        new BigDecimal("50.00"),
        TransactionType.EXPENSE,
        frequency,
        dayOfMonth,
        dayOfWeek,
        true,
        startDate,
        endDate
    );
  }

  private static Account buildAccount(final UUID accountSid, final UUID userSid) {
    final Account account = new Account();
    account.setSid(accountSid);
    account.setBaseCurrency(Currency.EUR);
    account.setStatus(AccountStatus.ACTIVE);
    account.setBalance(BigDecimal.ZERO);

    final AccountMembership membership = new AccountMembership();
    membership.setUserSid(userSid);
    membership.setRole(MembershipRole.OWNER);
    account.getMemberships().add(membership);

    return account;
  }

}
