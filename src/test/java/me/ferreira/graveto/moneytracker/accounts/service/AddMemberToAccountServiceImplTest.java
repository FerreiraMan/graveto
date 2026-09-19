package me.ferreira.graveto.moneytracker.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.MemberNotRegisteredException;
import me.ferreira.graveto.common.web.exception.moneytracker.UserAlreadyAccountMemberException;
import me.ferreira.graveto.identity.api.UserApi;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.command.AddMemberToAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.impl.AccountServiceImpl;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class AddMemberToAccountServiceImplTest {

  @InjectMocks
  private AccountServiceImpl service;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private UserApi userApi;

  @Test
  void shouldThrowWhenAccountDoesNotExist() {
    // Arrange
    final AddMemberToAccountCommand command = mock(AddMemberToAccountCommand.class);
    when(accountRepository.findBySid(any())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.addMember(command);
    }).isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToAddMemberToAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.CONTRIBUTOR);
    final AddMemberToAccountCommand command = mock(AddMemberToAccountCommand.class);

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.addMember(command);
    }).isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to add members for this account.");
  }

  @Test
  void shouldThrowIfNewMemberIsNotYetRegistered() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String email = "email@test.com";
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final AddMemberToAccountCommand command = new AddMemberToAccountCommand(
        userSid,
        UUID.randomUUID(),
        email,
        MembershipRole.CONTRIBUTOR
    );

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(userApi.fetchUserByEmail(email)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.addMember(command);
    }).isInstanceOf(MemberNotRegisteredException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          Assertions.assertThat(ae.getSafeMessage())
              .isEqualTo("New account member needs to be registered in the platform in order to be onboarded.");
        });
  }

  @Test
  void shouldThrowIfNewMemberIsAlreadyMemberOfAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final AddMemberToAccountCommand command = new AddMemberToAccountCommand(
        userSid,
        UUID.randomUUID(),
        "",
        MembershipRole.CONTRIBUTOR
    );

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(userApi.fetchUserByEmail("")).thenReturn(Optional.of(new UserResponseDto(userSid, "")));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.addMember(command);
    }).isInstanceOf(UserAlreadyAccountMemberException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          Assertions.assertThat(ae.getSafeMessage())
              .isEqualTo("This user is already a member of the account.");
        });
  }

  @Test
  void shouldAddNewMemberToAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID newMemberUserSid = UUID.randomUUID();
    final String email = "email@test.com";
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final AddMemberToAccountCommand command = new AddMemberToAccountCommand(
        userSid,
        account.getSid(),
        email,
        MembershipRole.CONTRIBUTOR
    );

    when(accountRepository.findBySid(any())).thenReturn(Optional.of(account));
    when(userApi.fetchUserByEmail(email)).thenReturn(Optional.of(new UserResponseDto(newMemberUserSid, email)));
    when(userApi.fetchUserDetailsByUserSids(Set.of(userSid, newMemberUserSid))).thenReturn(
        Map.of(userSid, new UserResponseDto(userSid, ""), newMemberUserSid,
            new UserResponseDto(newMemberUserSid, email)));

    // Act
    final AccountDetails updatedAccount = service.addMember(command);

    // Assert
    assertThat(updatedAccount.users().size()).isEqualTo(2);
    assertThat(updatedAccount.users().stream().map(AccountDetails.MembershipDetails::sid)).contains(userSid,
        newMemberUserSid);
    assertThat(updatedAccount.users())
        .filteredOn(u -> u.sid().equals(newMemberUserSid))
        .map(AccountDetails.MembershipDetails::email)
        .containsExactly(email);
  }

}
