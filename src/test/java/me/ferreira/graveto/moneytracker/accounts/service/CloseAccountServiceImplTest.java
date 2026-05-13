package me.ferreira.graveto.moneytracker.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountClosedEvent;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.impl.AccountServiceImpl;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class CloseAccountServiceImplTest {

  @InjectMocks
  private AccountServiceImpl service;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private AccountRepository accountRepository;

  @Test
  void shouldThrowWhenAccountDoesNotExist() {
    // Arrange
    final CloseAccountCommand command = new CloseAccountCommand(
        UUID.randomUUID(),
        UUID.randomUUID()
    );
    when(accountRepository.findBySid(any())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.closeAccount(command);
    }).isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToCloseAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final CloseAccountCommand command = mock(CloseAccountCommand.class);

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.closeAccount(command);
    }).isInstanceOf(InsufficientPermissionsException.class)
        .hasMessage("User does not have the required role to request closure for this account.");
  }

  @Test
  void shouldThrowIfAccountIsAlreadyClosed() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    account.setStatus(AccountStatus.CLOSED);
    final CloseAccountCommand command = mock(CloseAccountCommand.class);

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.closeAccount(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("This account is already closed.");
  }

  @Test
  void shouldThrowIfAccountStillHasBalance() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    account.setBalance(BigDecimal.ONE);
    final CloseAccountCommand command = mock(CloseAccountCommand.class);

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.closeAccount(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Account balance must be exactly 0.00 before it can be closed. Current balance: " + BigDecimal.ONE);
  }

  @Test
  void shouldCloseAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    account.setBalance(BigDecimal.ZERO);
    final CloseAccountCommand command = new CloseAccountCommand(userSid, account.getSid());

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));

    // Act
    final Account closedAccount = service.closeAccount(command);

    // Assert
    assertThat(closedAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
    verify(publisher, times(1)).publishEvent(new AccountClosedEvent(closedAccount));
  }

}
