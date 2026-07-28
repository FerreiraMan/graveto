package me.ferreira.graveto.moneytracker.transactions.service.recurringtransfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import me.ferreira.graveto.common.web.exception.moneytracker.RecurringTransferNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.UpdateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateRecurringTransferServiceImplTest {

  @InjectMocks
  private RecurringTransferServiceImpl recurringTransferService;
  @Mock
  private AccountService accountService;
  @Mock
  private RecurringTransferRepository recurringTransferRepository;

  @Test
  void shouldThrowWhenRecurringTransferNotFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.empty());

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.updateRecurringTransfer(command))
        .isInstanceOf(RecurringTransferNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);

    final UpdateRecurringTransferCommand command = buildCommand(otherUserSid, existingRt.getSid(),
        null, null, null, null, null, null, null, null, null);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.updateRecurringTransfer(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class);
  }

  @Test
  void shouldUpdateDescriptionAndAmount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        "Updated desc", new BigDecimal("99.99"), null, null, null, null, null, null, null);

    // Act
    final RecurringTransfer result = recurringTransferService.updateRecurringTransfer(command);

    // Assert
    assertThat(result.getDescription()).isEqualTo("Updated desc");
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    verify(recurringTransferRepository).save(existingRt);
  }

  @Test
  void shouldUpdateStatusToPausedAndNotRecalculateNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);
    final LocalDate originalNextExecution = existingRt.getNextExecutionDate();

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, RecurringOperationStatus.PAUSED, LocalDate.of(2030, 10, 1), null);

    // Act
    final RecurringTransfer result = recurringTransferService.updateRecurringTransfer(command);

    // Assert
    assertThat(result.getStatus()).isEqualTo(RecurringOperationStatus.PAUSED);
    assertThat(result.getNextExecutionDate()).isEqualTo(originalNextExecution);
  }

  @Test
  void shouldRecalculateNextExecutionDateWhenFrequencyChanges() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);
    existingRt.setDayOfTheWeek(3);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.WEEKLY, null, 3, null, null, null, null);

    // Act
    final RecurringTransfer result = recurringTransferService.updateRecurringTransfer(command);

    // Assert
    assertThat(result.getFrequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(3);
  }

  @Test
  void shouldUseExplicitNextExecutionDateWhenProvided() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);
    existingRt.setEndDate(LocalDate.of(2027, 12, 31));

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final LocalDate explicitDate = LocalDate.of(2026, 10, 1);
    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, explicitDate, null);

    // Act
    final RecurringTransfer result = recurringTransferService.updateRecurringTransfer(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isEqualTo(explicitDate);
  }

  @Test
  void shouldThrowWhenFrequencyChangesToMonthlyWithoutDayOfMonth() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);
    existingRt.setFrequency(Frequency.DAILY);
    existingRt.setDayOfTheMonth(null);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.MONTHLY, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.updateRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month needs to be provided when selecting monthly operation.");
  }

  @Test
  void shouldThrowWhenFrequencyChangesToWeeklyWithoutDayOfWeek() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);
    existingRt.setFrequency(Frequency.DAILY);
    existingRt.setDayOfTheWeek(null);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, Frequency.WEEKLY, null, null, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.updateRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
  }

  @Test
  void shouldKeepExistingValuesWhenFieldsAreNull() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt = buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount);

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    final UpdateRecurringTransferCommand command = buildCommand(userSid, rtSid,
        null, null, null, null, null, null, null, null, null);

    // Act
    final RecurringTransfer result = recurringTransferService.updateRecurringTransfer(command);

    // Assert
    assertThat(result.getDescription()).isEqualTo("Home Insurance");
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(result.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(result.getAdjustToBusinessDay()).isTrue();
  }

  private static UpdateRecurringTransferCommand buildCommand(final UUID userSid,
                                                             final UUID sid, final String description,
                                                             final BigDecimal amount, final Frequency frequency,
                                                             final Integer dayOfMonth, final Integer dayOfWeek,
                                                             final Boolean adjustToBusinessDay,
                                                             final RecurringOperationStatus status,
                                                             final LocalDate nextExecutionDate,
                                                             final LocalDate endDate) {
    return new UpdateRecurringTransferCommand(
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

  private static RecurringTransfer buildExistingRecurringTransfer(final UUID rtSid, final Account sourceAccount,
                                                                  final Account destinationAccount) {

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(rtSid);
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setUserSid(sourceAccount.getMemberships().getFirst().getUserSid());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
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
