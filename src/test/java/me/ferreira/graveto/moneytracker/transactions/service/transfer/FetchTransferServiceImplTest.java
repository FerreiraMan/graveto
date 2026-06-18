package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.FetchTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.transfer.TransferServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FetchTransferServiceImplTest {

  @InjectMocks
  private TransferServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void shouldThrowIfTransferTransactionsAreNotFoundDuringTransferFetch() {
    // Arrange
    final UUID correlationId = UUID.randomUUID();
    final FetchTransferCommand command = new FetchTransferCommand(UUID.randomUUID(), correlationId);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of());

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchTransfer(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Transfer is associated with an incorrect amount of transactions.");
  }

  @Test
  void shouldThrowIfTransferTransactionsAreNotOfCorrectTypeDuringTransferFetch() {
    // Arrange
    final UUID correlationId = UUID.randomUUID();
    final FetchTransferCommand command = new FetchTransferCommand(UUID.randomUUID(), correlationId);

    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);

    // Corrupted twin!
    final Transaction txInvalid = new Transaction();
    txInvalid.setType(TransactionType.INCOME);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txInvalid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchTransfer(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Corrupted transfer does not contain exactly one IN and one OUT transaction.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToFetchTransfer() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final FetchTransferCommand command = new FetchTransferCommand(userSid, correlationId);

    final Account unauthorizedOutAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);

    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(unauthorizedOutAccount);

    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txOut, txIn));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.fetchTransfer(command);
    }).isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to read transfer transactions for this account.");
  }

  @Test
  void shouldSuccessfullyFetchTransfer() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID correlationId = UUID.randomUUID();
    final FetchTransferCommand command = new FetchTransferCommand(userSid, correlationId);

    final Account authorizedOutAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final Account inAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);

    final BigDecimal transferAmount = BigDecimal.valueOf(150);

    final Transaction txOut = new Transaction();
    txOut.setType(TransactionType.TRANSFER_OUT);
    txOut.setAccount(authorizedOutAccount);
    txOut.setAmount(transferAmount);
    txOut.setCorrelationId(correlationId);

    final Transaction txIn = new Transaction();
    txIn.setType(TransactionType.TRANSFER_IN);
    txIn.setAccount(inAccount);
    txIn.setAmount(transferAmount);
    txIn.setCorrelationId(correlationId);

    when(transactionRepository.findAllByCorrelationId(correlationId)).thenReturn(List.of(txIn, txOut));

    // Act
    final TransferResult result = service.fetchTransfer(command);

    // Assert
    assertThat(result.expense()).isEqualTo(txOut);
    assertThat(result.expense().getAmount()).isEqualByComparingTo(transferAmount);
    assertThat(result.expense().getAccount()).isEqualTo(authorizedOutAccount);

    assertThat(result.income()).isEqualTo(txIn);
    assertThat(result.income().getAmount()).isEqualByComparingTo(transferAmount);
    assertThat(result.income().getAccount()).isEqualTo(inAccount);
  }

}
