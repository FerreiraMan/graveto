package me.ferreira.graveto.moneytracker.transactions.service.transactiongroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.common.BusinessRuleViolationException;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionGroupServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.payload.UngroupDetails;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.TransactionGroupUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class UngroupServiceImplTest {

  @InjectMocks
  private TransactionGroupServiceImpl service;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private TransactionGroupRepository transactionGroupRepository;
  @Mock
  private TransactionGroupAccountRepository transactionGroupAccountRepository;

  private UUID userSid;
  private UUID accountSid;
  private Account account;

  @BeforeEach
  void setUp() {
    userSid = UUID.randomUUID();
    accountSid = UUID.randomUUID();
    account = AccountUtils.createAccount(accountSid, userSid, MembershipRole.OWNER);
  }

  @Test
  void shouldThrowIfAccountIsNotFoundDuringUngroup() {
    // Arrange
    final UngroupCommand command = new UngroupCommand(userSid, accountSid, Set.of(UUID.randomUUID()));
    when(accountService.fetchAccountEntity(accountSid)).thenThrow(new AccountNotFoundException("loggableMessage"));

    // Act & Assert
    assertThatThrownBy(() -> service.ungroup(command))
        .isInstanceOf(AccountNotFoundException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(ae.getSafeMessage()).isEqualTo(
              "The specified account is no longer available. " +
                  "It may have been removed, or you may not have access to it.");
        });

    verifyNoInteractions(transactionService, transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldThrowIfUserCannotUpdateTransactionsDuringUngroup() {
    // Arrange
    final UUID strangerSid = UUID.randomUUID();
    final UngroupCommand command = new UngroupCommand(strangerSid, accountSid, Set.of(UUID.randomUUID()));
    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);

    // Act & Assert
    assertThatThrownBy(() -> service.ungroup(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(ae.getSafeMessage()).isEqualTo("You do not have the required role to perform this action.");
        });

    verifyNoInteractions(transactionService, transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldThrowIfSomeTransactionsAreMissingDuringUngroup() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final UngroupCommand command = new UngroupCommand(userSid, accountSid, requested);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(TransactionGroupUtils.createTransaction(null)));

    // Act & Assert
    assertThatThrownBy(() -> service.ungroup(command))
        .isInstanceOf(BusinessRuleViolationException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
          assertThat(ae.getSafeMessage())
              .isEqualTo("Unable to process the request due to missing or deleted transactions.");
        });
  }

  @Test
  void shouldRemoveGroupedTransactionsAndCountAlreadyUngroupedOnes() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    final TransactionGroup birthday = TransactionGroupUtils.createGroup("Birthday");
    final TransactionGroup algarve = TransactionGroupUtils.createGroup("Algarve");
    final Transaction inBirthday = TransactionGroupUtils.createTransaction(birthday);
    final Transaction inAlgarve = TransactionGroupUtils.createTransaction(algarve);
    final Transaction ungrouped = TransactionGroupUtils.createTransaction(null);
    final UngroupCommand command = new UngroupCommand(userSid, accountSid, requested);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(List.of(inBirthday, inAlgarve, ungrouped));

    // Act
    final UngroupDetails details = service.ungroup(command);

    // Assert
    assertThat(details.removed()).isEqualTo(2);
    assertThat(details.alreadyUngrouped()).isEqualTo(1);
    assertThat(List.of(inBirthday, inAlgarve, ungrouped))
        .allSatisfy(t -> assertThat(t.getTransactionGroup()).isNull());
    verifyNoInteractions(transactionGroupRepository, transactionGroupAccountRepository);
  }

  @Test
  void shouldReportZeroRemovedWhenNoTransactionIsGrouped() {
    // Arrange
    final Set<UUID> requested = Set.of(UUID.randomUUID(), UUID.randomUUID());
    final List<Transaction> transactions = List.of(
        TransactionGroupUtils.createTransaction(null), TransactionGroupUtils.createTransaction(null));
    final UngroupCommand command = new UngroupCommand(userSid, accountSid, requested);

    when(accountService.fetchAccountEntity(accountSid)).thenReturn(account);
    when(transactionService.findAllBySidFromAccountAndStatus(requested, accountSid, TransactionStatus.ACTIVE))
        .thenReturn(transactions);

    // Act
    final UngroupDetails details = service.ungroup(command);

    // Assert
    assertThat(details.removed()).isZero();
    assertThat(details.alreadyUngrouped()).isEqualTo(2);
  }

}
