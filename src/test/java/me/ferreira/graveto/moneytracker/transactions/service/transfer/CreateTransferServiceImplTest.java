package me.ferreira.graveto.moneytracker.transactions.service.transfer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.moneytracker.AccountNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsOnAccountException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransferServiceImpl;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateTransferServiceImplTest {

  @InjectMocks
  private TransferServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  @Test
  void shouldThrowIfSourceAccountIsSameAsDestinationAccount() {
    // Arrange
    final UUID sourceAccountSid = UUID.fromString("596f0f38-e480-476c-abc3-34181bf74a15");
    final UUID destinationAccountSid = UUID.fromString("596f0f38-e480-476c-abc3-34181bf74a15");

    final CreateTransferCommand command = new CreateTransferCommand(
        UUID.randomUUID(),
        sourceAccountSid,
        destinationAccountSid,
        null,
        null,
        null
    );

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createTransfer(command);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Source and destination accounts cannot be the same.");
  }

  @Test
  void shouldThrowIfSourceAccountIsNotFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceSid = UUID.randomUUID();
    final UUID destSid = UUID.randomUUID();

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid, sourceSid, destSid, BigDecimal.TEN, "Test", LocalDateTime.now()
    );

    when(accountService.fetchAccountEntity(sourceSid)).thenThrow(new AccountNotFoundException(sourceSid));

    // Act & Assert
    assertThatThrownBy(() -> service.createTransfer(command))
        .isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account with SID [" + sourceSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldThrowIfAccountIsNotActiveDuringTransferCreation() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceSid = UUID.randomUUID();
    final UUID destSid = UUID.randomUUID();

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid, sourceSid, destSid, BigDecimal.TEN, "Test", LocalDateTime.now()
    );

    final Account sourceAccount = AccountUtils.createAccount(sourceSid, userSid, null);
    sourceAccount.setStatus(AccountStatus.CLOSED);

    when(accountService.fetchAccountEntity(sourceSid)).thenReturn(sourceAccount);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.createTransfer(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create transfer transactions. The account is currently CLOSED.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedOnSourceAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceSid = UUID.randomUUID();
    final UUID destSid = UUID.randomUUID();

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid, sourceSid, destSid, BigDecimal.TEN, "Test", LocalDateTime.now()
    );

    final Account sourceAccount = AccountUtils.createAccount(sourceSid, userSid, null);

    when(accountService.fetchAccountEntity(sourceSid)).thenReturn(sourceAccount);
    when(accountService.fetchAccountEntity(destSid)).thenReturn(sourceAccount);

    // Act & Assert
    assertThatThrownBy(() -> service.createTransfer(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to create transfer transactions for this account.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedOnDestinationAccount() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID sourceSid = UUID.randomUUID();
    final UUID destSid = UUID.randomUUID();

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid, sourceSid, destSid, BigDecimal.TEN, "Test", LocalDateTime.now()
    );

    final Account sourceAccount = AccountUtils.createAccount(sourceSid, userSid, MembershipRole.OWNER);
    final Account destAccount = AccountUtils.createAccount(destSid, userSid, null);

    when(accountService.fetchAccountEntity(sourceSid)).thenReturn(sourceAccount);
    when(accountService.fetchAccountEntity(destSid)).thenReturn(destAccount);

    // Act & Assert
    assertThatThrownBy(() -> service.createTransfer(command))
        .isInstanceOf(InsufficientPermissionsOnAccountException.class)
        .hasMessage("User does not have the required role to create transfer transactions for this account.");
  }

  @Test
  void shouldSuccessfullyCreateTransferAndUpdateBalances() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final BigDecimal transferAmount = BigDecimal.TEN;
    final LocalDateTime occurredAt = LocalDateTime.now();
    final String description = "Monthly Savings";

    final Account sourceAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    sourceAccount.setBalance(new BigDecimal("100.00"));

    final Account destAccount = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    destAccount.setBalance(new BigDecimal("50.00"));

    final Category outCategory =
        CategoryUtils.createCategory("Transfer Out", null, null, true, TransactionType.TRANSFER_OUT);
    final Category inCategory =
        CategoryUtils.createCategory("Transfer In", null, null, true, TransactionType.TRANSFER_IN);

    final CreateTransferCommand command = new CreateTransferCommand(
        userSid, sourceAccount.getSid(), destAccount.getSid(), transferAmount, description, occurredAt
    );

    when(accountService.fetchAccountEntity(sourceAccount.getSid())).thenReturn(sourceAccount);
    when(accountService.fetchAccountEntity(destAccount.getSid())).thenReturn(destAccount);

    when(categoryService.fetchInternalCategory(SystemCategory.TRANSFER_OUT.getSid())).thenReturn(outCategory);
    when(categoryService.fetchInternalCategory(SystemCategory.TRANSFER_IN.getSid())).thenReturn(inCategory);

    // Act
    final TransferResult result = service.createTransfer(command);

    // Assert
    assertThat(sourceAccount.getBalance()).isEqualByComparingTo(new BigDecimal("90.00"));
    assertThat(destAccount.getBalance()).isEqualByComparingTo(new BigDecimal("60.00"));

    @SuppressWarnings("unchecked") final ArgumentCaptor<List<Transaction>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(transactionRepository).saveAll(listCaptor.capture());

    final List<Transaction> savedTransactions = listCaptor.getValue();
    assertThat(savedTransactions.size()).isEqualTo(2);

    final Transaction savedOut = savedTransactions.get(0);
    final Transaction savedIn = savedTransactions.get(1);

    assertThat(savedOut.getCorrelationId()).isNotNull();
    assertThat(savedOut.getCorrelationId()).isEqualTo(savedIn.getCorrelationId());

    assertThat(result.expense()).isEqualTo(savedOut);
    assertThat(result.income()).isEqualTo(savedIn);

    assertThat(savedOut.getAccount()).isEqualTo(sourceAccount);
    assertThat(savedOut.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
    assertThat(savedOut.getCategory()).isEqualTo(outCategory);
    assertThat(savedOut.getAmount()).isEqualByComparingTo(transferAmount);
    assertThat(savedOut.getDescription()).isEqualTo(description);
    assertThat(savedOut.getOccurredAt()).isEqualTo(occurredAt);

    assertThat(savedIn.getAccount()).isEqualTo(destAccount);
    assertThat(savedIn.getType()).isEqualTo(TransactionType.TRANSFER_IN);
    assertThat(savedIn.getCategory()).isEqualTo(inCategory);
    assertThat(savedIn.getAmount()).isEqualByComparingTo(transferAmount);
    assertThat(savedIn.getDescription()).isEqualTo(description);
    assertThat(savedIn.getOccurredAt()).isEqualTo(occurredAt);
  }

}
