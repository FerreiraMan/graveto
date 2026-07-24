package me.ferreira.graveto.moneytracker.transactions.service.recurringtransfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CreateRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateRecurringTransferServiceImplTest {

  @InjectMocks
  private RecurringTransferServiceImpl recurringTransferService;
  @Mock
  private AccountService accountService;
  @Mock
  private RecurringTransferRepository recurringTransferRepository;

  @Test
  void shouldThrowWhenMonthlyFrequencyAndDayOfMonthIsNull() {
    // Arrange
    final CreateRecurringTransferCommand
        command = buildCommand(
        Frequency.MONTHLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month needs to be provided when selecting monthly operation.");

    verify(recurringTransferRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenWeeklyFrequencyAndDayOfWeekIsNull() {
    // Arrange
    final CreateRecurringTransferCommand command = buildCommand(
        Frequency.WEEKLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");

    verify(recurringTransferRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenBiWeeklyFrequencyAndDayOfWeekIsNull() {
    // Arrange
    final CreateRecurringTransferCommand command = buildCommand(
        Frequency.BI_WEEKLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
  }

  @Test
  void shouldThrowWhenAnnuallyAndBothDayOfMonthAndStartDateAreNull() {
    // Arrange
    final CreateRecurringTransferCommand command = buildCommand(
        Frequency.ANNUALLY, null, null, null, null);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Day of the month needs to be provided when selecting annual operation.");
  }

  @Test
  void shouldThrowWhenEndDateIsBeforeStartDate() {
    // Arrange
    final CreateRecurringTransferCommand command = buildCommand(
        Frequency.MONTHLY, 15, null, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 1));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("End date must be after start date.");
  }

  @Test
  void shouldThrowWhenSourceAccountAndDestinationAccountAreEqual() {
    // Arrange
    final UUID sameAccountSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = new CreateRecurringTransferCommand(
        UUID.randomUUID(),
        sameAccountSid,
        sameAccountSid,
        "Transfer",
        BigDecimal.ONE,
        Frequency.MONTHLY,
        15,
        null,
        true,
        LocalDate.of(2046, 8, 15),
        null
    );

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Source and destination accounts cannot be the same.");
  }

  @Test
  void shouldThrowWhenAccountIsNotActive() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, null, null);

    final Account account = new Account();
    account.setSid(command.sourceAccountSid());
    account.setStatus(AccountStatus.CLOSED);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldThrowWhenUserDoesNotHavePermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        UUID.randomUUID(), Frequency.MONTHLY, 15, null, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.createRecurringTransfer(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class);
  }

  @Test
  void shouldCreateRecurringTransferSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final LocalDate startDate = LocalDate.of(2026, 8, 15);
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, startDate, null);

    final Account sourceAccount = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(command.sourceAccountSid())).thenReturn(sourceAccount);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(command.destinationAccountSid())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    final ArgumentCaptor<RecurringTransfer> captor = ArgumentCaptor.forClass(RecurringTransfer.class);
    verify(recurringTransferRepository).save(captor.capture());

    final RecurringTransfer saved = captor.getValue();
    assertThat(saved.getSid()).isNotNull();
    assertThat(saved.getSourceAccount()).isEqualTo(sourceAccount);
    assertThat(saved.getDestinationAccount()).isEqualTo(destinationAccount);
    assertThat(saved.getUserSid()).isEqualTo(userSid);
    assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
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
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.MONTHLY, 15, null, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    assertThat(result.getStartDate()).isNotNull();
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getStartDate()).isEqualTo(result.getNextExecutionDate());
  }

  @Test
  void shouldResolveStartDateForWeeklyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.WEEKLY, null, 1, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(1);
  }

  @Test
  void shouldResolveStartDateForBiWeeklyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.BI_WEEKLY, null, 5, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfWeek().getValue()).isEqualTo(5);
  }

  @Test
  void shouldResolveStartDateForAnnuallyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.ANNUALLY, 20, null, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate().getDayOfMonth()).isEqualTo(20);
  }

  @Test
  void shouldResolveStartDateForDailyFrequency() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateRecurringTransferCommand command = buildCommandWithUser(
        userSid, Frequency.DAILY, null, null, null, null);

    final Account account = buildAccount(command.sourceAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(account);
    final Account destinationAccount = buildAccount(command.destinationAccountSid(), userSid);
    when(accountService.fetchAccountEntity(any())).thenReturn(destinationAccount);
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result = recurringTransferService.createRecurringTransfer(command);

    // Assert
    assertThat(result.getNextExecutionDate()).isNotNull();
    assertThat(result.getNextExecutionDate()).isAfter(LocalDate.now());
  }

  private static CreateRecurringTransferCommand buildCommand(final Frequency frequency,
                                                                final Integer dayOfMonth,
                                                                final Integer dayOfWeek,
                                                                final LocalDate startDate,
                                                                final LocalDate endDate) {
    return buildCommandWithUser(UUID.randomUUID(), frequency, dayOfMonth, dayOfWeek, startDate, endDate);
  }

  private static CreateRecurringTransferCommand buildCommandWithUser(final UUID userSid,
                                                                        final Frequency frequency,
                                                                        final Integer dayOfMonth,
                                                                        final Integer dayOfWeek,
                                                                        final LocalDate startDate,
                                                                        final LocalDate endDate) {
    return new CreateRecurringTransferCommand(
        userSid,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Home Insurance",
        new BigDecimal("50.00"),
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
