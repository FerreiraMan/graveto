package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.DeleteTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.transfer.TransferServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteTransferServiceImplTest {

  @InjectMocks
  private TransferServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void shouldThrowIfTransferTransactionsAreNotFoundDuringTransferDeletion() {
    // Arrange
    when(transactionRepository.findAllByCorrelationId(any())).thenReturn(List.of());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransfer(mock(DeleteTransferCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Transfer is associated with an incorrect amount of transactions.");
  }

  @Test
  void shouldThrowIfTransferTransactionsAreNotOfCorrectType() {
    // Arrange
    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.EXPENSE);

    when(transactionRepository.findAllByCorrelationId(any())).thenReturn(List.of(txOut, txIn));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransfer(mock(DeleteTransferCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Corrupted transfer does not contain exactly one IN and one OUT transaction.");
  }

  @Test
  void shouldThrowIfAccountIsNotActiveDuringTransferDeletion() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    account.setStatus(AccountStatus.CLOSED);
    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(account);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setAccount(account);

    when(transactionRepository.findAllByCorrelationId(any())).thenReturn(List.of(txOut, txIn));
    final DeleteTransferCommand command = mock(DeleteTransferCommand.class);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransfer(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot delete transfer transactions. The account is currently CLOSED.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToDeleteTransferTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(account);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setAccount(account);

    when(transactionRepository.findAllByCorrelationId(any())).thenReturn(List.of(txOut, txIn));
    final DeleteTransferCommand command = mock(DeleteTransferCommand.class);

    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransfer(command);
    }).isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to delete transfer transactions for this account.");
  }

  @Test
  void shouldThrowIfTransferTransactionIsAlreadyDeleted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final DeleteTransferCommand command = new DeleteTransferCommand(userSid, correlationId);

    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(account);
    txOut.setStatus(TransactionStatus.DELETED);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setStatus(TransactionStatus.DELETED);
    txIn.setAccount(account);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.deleteTransfer(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Transaction is already deleted.");
  }

  @Test
  void shouldMarkTransferTransactionsAsDeleted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final DeleteTransferCommand command = new DeleteTransferCommand(userSid, correlationId);


    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(account);
    txOut.setAmount(BigDecimal.TEN);
    txOut.setStatus(TransactionStatus.ACTIVE);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setStatus(TransactionStatus.ACTIVE);
    txIn.setAmount(BigDecimal.TEN);
    txIn.setAccount(account);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

    // Act
    final TransferResult deletedTransfer = service.deleteTransfer(command);

    // Assert
    assertThat(deletedTransfer.expense().getStatus()).isEqualTo(TransactionStatus.DELETED);
    assertThat(deletedTransfer.expense().getDeletedAt()).isNotNull();
    assertThat(deletedTransfer.income().getStatus()).isEqualTo(TransactionStatus.DELETED);
    assertThat(deletedTransfer.income().getDeletedAt()).isNotNull();
  }

  @Test
  void shouldRevertBalanceOnAccountsAfterTransferTransactionsDeletion() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final Account outAccount =
        AccountUtils.createAccount(UUID.randomUUID(), userSid, BigDecimal.valueOf(100), MembershipRole.OWNER);
    final Account inAccount =
        AccountUtils.createAccount(UUID.randomUUID(), userSid, BigDecimal.valueOf(200), MembershipRole.OWNER);
    final DeleteTransferCommand command = new DeleteTransferCommand(userSid, correlationId);

    final BigDecimal transferAmount = BigDecimal.valueOf(50);

    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(outAccount);
    txOut.setAmount(transferAmount);
    txOut.setStatus(TransactionStatus.ACTIVE);
    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setStatus(TransactionStatus.ACTIVE);
    txIn.setAmount(transferAmount);
    txIn.setAccount(inAccount);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

    // Act
    service.deleteTransfer(command);

    // Assert
    assertThat(outAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100).add(transferAmount));
    assertThat(inAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(200).subtract(transferAmount));
  }

}
