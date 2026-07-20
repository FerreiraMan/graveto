package me.ferreira.graveto.moneytracker.transactions.service.recurringtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.RecurringTransactionNotFoundException;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateRecurringTransactionServiceImplTest {

  @InjectMocks
  private RecurringTransactionServiceImpl recurringTransactionService;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private RecurringTransactionRepository recurringTransactionRepository;

  @Test
  void shouldThrowWhenRecurringTransactionNotFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.empty());

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(command))
        .isInstanceOf(RecurringTransactionNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);

    final UpdateRecurringTransactionCommand command = buildCommand(otherUserSid, existingRt.getSid(),
        null, null, null, null, null, null, null, null, null);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class);
  }

  @Test
  void shouldUpdateDescriptionAndAmount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        "Updated desc", new BigDecimal("99.99"), null, null, null, null, null, null, null);

    // Act
    final RecurringTransaction result = recurringTransactionService.updateRecurringTransaction(command);

    // Assert
    assertThat(result.getDescription()).isEqualTo("Updated desc");
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    verify(recurringTransactionRepository).save(existingRt);
  }

  @Test
  void shouldUpdateStatusToPausedAndNotRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);
    final LocalDate originalNextExecution = existingRt.getNextExecutionDate();

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, RecurringOperationStatus.PAUSED, null, null);

    // Act
    final RecurringTransaction result = recurringTransactionService.updateRecurringTransaction(command);

    // Assert
    assertThat(result.getStatus()).isEqualTo(RecurringOperationStatus.PAUSED);
    assertThat(result.getNextExecutionDate()).isEqualTo(originalNextExecution);
  }

  @Test
  void shouldRecalculateNextExecutionDateWhenFrequencyChanges() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);
    existingRt.setDayOfTheWeek(3);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.WEEKLY, null, 3, null, null, null, null);

    // Act
    final RecurringTransaction result = recurringTransactionService.updateRecurringTransaction(command);

    // Assert
    assertThat(result.getFrequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(3);
  }

  @Test
  void shouldUseExplicitNextExecutionDateWhenProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);
    existingRt.setEndDate(LocalDate.of(2027, 12, 31));

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final LocalDate explicitDate = LocalDate.of(2026, 10, 1);
    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, explicitDate, null);

    // Act
    final RecurringTransaction result = recurringTransactionService.updateRecurringTransaction(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isEqualTo(explicitDate);
  }

  @Test
  void shouldThrowWhenFrequencyChangesToMonthlyWithoutDayOfMonth() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);
    existingRt.setFrequency(Frequency.DAILY);
    existingRt.setDayOfTheMonth(null);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.MONTHLY, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month needs to be provided when selecting monthly operation.");
  }

  @Test
  void shouldThrowWhenFrequencyChangesToWeeklyWithoutDayOfWeek() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);
    existingRt.setFrequency(Frequency.DAILY);
    existingRt.setDayOfTheWeek(null);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.WEEKLY, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransactionService.updateRecurringTransaction(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
  }

  @Test
  void shouldKeepExistingValuesWhenFieldsAreNull() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final Account account = buildAccount(accountSid, userSid);
    final RecurringTransaction existingRt = buildExistingRecurringTransaction(rtSid, account);

    when(recurringTransactionRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransactionCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, null, null);

    // Act
    final RecurringTransaction result = recurringTransactionService.updateRecurringTransaction(command);

    // Assert
    assertThat(result.getDescription()).isEqualTo("Home Insurance");
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(result.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(result.getAdjustToBusinessDay()).isTrue();
  }

  private static UpdateRecurringTransactionCommand buildCommand(final UUID userSid,
                                                                final UUID sid, final String description,
                                                                final BigDecimal amount, final Frequency frequency,
                                                                final Integer dayOfMonth, final Integer dayOfWeek,
                                                                final Boolean adjustToBusinessDay,
                                                                final RecurringOperationStatus status,
                                                                final LocalDate nextExecutionDate,
                                                                final LocalDate endDate) {
    return new UpdateRecurringTransactionCommand(
        userSid, sid, description, amount, frequency, dayOfMonth, dayOfWeek,
        adjustToBusinessDay, status, nextExecutionDate, endDate);
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

  private static RecurringTransaction buildExistingRecurringTransaction(final UUID rtSid, final Account account) {
    final Category category = new Category();
    category.setSid(UUID.randomUUID());
    category.setDisplayName("Insurance");

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(rtSid);
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(account.getMemberships().getFirst().getUserSid());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setType(TransactionType.EXPENSE);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setDayOfTheMonth(15);
    rt.setDayOfTheWeek(null);
    rt.setAdjustToBusinessDay(true);
    rt.setNextExecutionDate(LocalDate.of(2026, 8, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setStartDate(LocalDate.of(2026, 7, 15));
    rt.setEndDate(null);
    return rt;
  }

}
