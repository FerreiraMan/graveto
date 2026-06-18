package me.ferreira.graveto.moneytracker.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionServiceImpl;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteTransactionServiceImplTest {

  @InjectMocks
  private TransactionServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  private static Stream<Arguments> resultingBalanceAccordingToType() {
    return Stream.of(
        Arguments.of(TransactionType.EXPENSE, BigDecimal.valueOf(11)),
        Arguments.of(TransactionType.INCOME, BigDecimal.valueOf(9))
    );
  }

  @Test
  void shouldThrowIfTransactionIsNotFoundDuringTransactionDeletion() {
    // Arrange
    final UUID transactionSid = UUID.randomUUID();

    when(transactionRepository.findBySid(any())).thenThrow(new TransactionNotFoundException(transactionSid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(Mockito.mock(DeleteTransactionCommand.class));
    }).isInstanceOf(TransactionNotFoundException.class)
        .hasMessage("Transaction with SID [" + transactionSid + "] was not found.");
  }

  @Test
  void shouldThrowIfTransactionHasCorrelationIdDuringTransactionDeletion() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final UUID transactionSid = UUID.randomUUID();
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setSid(transactionSid);
    transaction.setCorrelationId(UUID.randomUUID());

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(Mockito.mock(DeleteTransactionCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("This transaction is part of a transfer and must be deleted via the Transfer API.");
  }

  @Test
  void shouldThrowIfAccountIsNotActiveDuringTransactionDeletion() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    account.setStatus(AccountStatus.CLOSED);
    final UUID transactionSid = UUID.randomUUID();
    final Transaction transaction = new Transaction();
    transaction.setSid(transactionSid);
    transaction.setCorrelationId(UUID.randomUUID());
    transaction.setAccount(account);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(Mockito.mock(DeleteTransactionCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot delete transactions. The account is currently CLOSED.");
  }

  @Test
  void shouldThrowIfTransactionIsOfTypeTransferDuringTransactionDeletion() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final UUID transactionSid = UUID.randomUUID();
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setSid(transactionSid);
    transaction.setType(TransactionType.TRANSFER_IN);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(Mockito.mock(DeleteTransactionCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("This transaction is part of a transfer and must be deleted via the Transfer API.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToDeleteTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final DeleteTransactionCommand command = mock(DeleteTransactionCommand.class);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(command);
    }).isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to delete transactions for this account.");
  }

  @Test
  void shouldThrowIfTransactionIsAlreadyDeleted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final DeleteTransactionCommand command = new DeleteTransactionCommand(userSid, transactionSid);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setStatus(TransactionStatus.DELETED);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransaction(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Transaction is already deleted.");
  }

  @Test
  void shouldMarkTransactionAsDeleted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final DeleteTransactionCommand command = new DeleteTransactionCommand(userSid, transactionSid);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setAmount(BigDecimal.TEN);
    transaction.setStatus(TransactionStatus.ACTIVE);
    transaction.setType(TransactionType.EXPENSE);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act
    final Transaction deletedTransaction = service.deleteTransaction(command);

    // Assert
    assertThat(deletedTransaction.getStatus()).isEqualTo(TransactionStatus.DELETED);
    assertThat(deletedTransaction.getDeletedAt()).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("resultingBalanceAccordingToType")
  void shouldRevertBalanceOnAccountAfterTransactionDeletion(final TransactionType type,
                                                            final BigDecimal resultingBalance) {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final DeleteTransactionCommand command = new DeleteTransactionCommand(userSid, transactionSid);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setAmount(BigDecimal.ONE);
    transaction.setStatus(TransactionStatus.ACTIVE);
    transaction.setType(type);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act
    service.deleteTransaction(command);

    // Assert
    assertThat(account.getBalance()).isEqualByComparingTo(resultingBalance);
  }

}
