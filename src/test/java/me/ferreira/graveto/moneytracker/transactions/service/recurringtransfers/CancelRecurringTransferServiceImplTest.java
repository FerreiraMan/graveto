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
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.CancelRecurringTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.RecurringTransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CancelRecurringTransferServiceImplTest {

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

    final CancelRecurringTransferCommand command = buildCommand(userSid, rtSid);

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.cancelRecurringTransfer(command))
        .isInstanceOf(RecurringTransferNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final RecurringTransfer existingRt =
        buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount, null);

    final CancelRecurringTransferCommand command = buildCommand(otherUserSid, existingRt.getSid());

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));

    // Act & Assert
    assertThatThrownBy(() -> recurringTransferService.cancelRecurringTransfer(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class);
  }

  @Test
  void shouldCancelRecurringTransfer() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID rtSid = UUID.randomUUID();
    final UUID sourceAccountSid = UUID.randomUUID();
    final UUID destinationAccountSid = UUID.randomUUID();
    final Account sourceAccount = buildAccount(sourceAccountSid, userSid);
    final Account destinationAccount = buildAccount(destinationAccountSid, userSid);
    final LocalDate originalEndDate = LocalDate.of(2048, 1, 20);
    final RecurringTransfer existingRt =
        buildExistingRecurringTransfer(rtSid, sourceAccount, destinationAccount, originalEndDate);

    final CancelRecurringTransferCommand command = buildCommand(userSid, existingRt.getSid());

    when(recurringTransferRepository.findBySid(rtSid)).thenReturn(Optional.of(existingRt));
    when(recurringTransferRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final RecurringTransfer result =
        recurringTransferService.cancelRecurringTransfer(command);

    // Assert
    assertThat(result.getStatus()).isEqualTo(RecurringOperationStatus.CANCELED);
    assertThat(result.getEndDate()).isNotEqualTo(originalEndDate);
    verify(recurringTransferRepository).save(existingRt);
  }

  private static CancelRecurringTransferCommand buildCommand(final UUID userSid, final UUID sid) {
    return new CancelRecurringTransferCommand(
        userSid, sid);
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
                                                                  final Account destinationAccount,
                                                                  final LocalDate endDate) {

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(rtSid);
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setEndDate(endDate);
    return rt;
  }

}
