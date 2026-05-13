package me.ferreira.graveto.moneytracker.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.impl.TransactionServiceImpl;
import me.ferreira.graveto.moneytracker.utils.AccountUtils;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
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
public class UpdateTransactionServiceImplTest {

  @InjectMocks
  private TransactionServiceImpl service;
  @Mock
  private CategoryService categoryService;
  @Mock
  private AccountService accountService;
  @Mock
  private TransactionRepository transactionRepository;

  private static Stream<Arguments> updateTransactionPayload() {

    final UUID userSid = UUID.randomUUID();
    final UUID txSid = UUID.randomUUID();
    final UUID newCategorySid = UUID.randomUUID();

    return Stream.of(
        // CASE 1: Change Amount Only (Expense goes from $10 to $30)
        // Math: Reverse $10 Expense (+10) -> Apply $30 Expense (-30) -> Net change: -20.
        // Initial Balance 100 -> Expected Balance 80
        Arguments.of(
            TransactionType.EXPENSE, new BigDecimal("10.00"), // Persisted state
            new UpdateTransactionCommand(userSid, txSid, null, null, new BigDecimal("30.00"), null, null),
            new BigDecimal("80.00"), // Expected Balance
            new BigDecimal("30.00"), // Expected Amount
            TransactionType.EXPENSE  // Expected Type
        ),

        // CASE 2: Change Type Only (Expense of $10 becomes Income of $10)
        // Math: Reverse $10 Expense (+10) -> Apply $10 Income (+10) -> Net change: +20.
        // Initial Balance 100 -> Expected Balance 120
        Arguments.of(
            TransactionType.EXPENSE, new BigDecimal("10.00"),
            new UpdateTransactionCommand(userSid, txSid, TransactionType.INCOME, null, null, null, null),
            new BigDecimal("120.00"),
            new BigDecimal("10.00"),
            TransactionType.INCOME
        ),

        // CASE 3: Change Both Amount And Type (Expense of $10 becomes Income of $20)
        // Math: Reverse $10 Expense (+10) -> Apply $30 Income (+30) -> Net change: +40.
        // Initial Balance 100 -> Expected Balance 140
        Arguments.of(
            TransactionType.EXPENSE, new BigDecimal("10.00"),
            new UpdateTransactionCommand(userSid, txSid, TransactionType.INCOME, null, new BigDecimal("30.00"), null,
                null),
            new BigDecimal("140.00"),
            new BigDecimal("30.00"),
            TransactionType.INCOME
        ),

        // CASE 4: No Math Change (Just update description and category)
        // Math: No change.
        // Initial Balance 100 -> Expected Balance 100
        Arguments.of(
            TransactionType.EXPENSE, new BigDecimal("10.00"),
            new UpdateTransactionCommand(userSid, txSid, null, newCategorySid, null, "Updated Description", null),
            new BigDecimal("100.00"),
            new BigDecimal("10.00"),
            TransactionType.EXPENSE
        )
    );
  }

  @Test
  void shouldThrowIfTransactionIsNotFoundDuringTransactionUpdate() {
    // Arrange
    final UUID transactionSid = UUID.randomUUID();

    when(transactionRepository.findBySid(any())).thenThrow(new TransactionNotFoundException(transactionSid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(Mockito.mock(UpdateTransactionCommand.class));
    }).isInstanceOf(TransactionNotFoundException.class)
        .hasMessage("Transaction with SID [" + transactionSid + "] was not found.");
  }

  @Test
  void shouldThrowIfAccountIsNotActiveDuringTransactionUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    account.setStatus(AccountStatus.CLOSED);

    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, transactionSid, TransactionType.EXPENSE, null, null, null, null);


    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setAmount(BigDecimal.ONE);
    transaction.setStatus(TransactionStatus.DELETED);
    transaction.setType(TransactionType.EXPENSE);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot update transactions. The account is currently CLOSED.");
  }

  @Test
  void shouldThrowIfTransactionIsPartOfTransferDuringTransactionUpdate() {
    // Arrange
    final Transaction transaction = new Transaction();
    transaction.setCorrelationId(UUID.randomUUID());

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(Mockito.mock(UpdateTransactionCommand.class));
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("This transaction is part of a transfer and must be updated via the Transfer API.");
  }

  @Test
  void shouldThrowIfAttemptToUpdateToTransferInTypeTransactionUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Transaction transaction = new Transaction();
    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, null, TransactionType.TRANSFER_IN, null, null, null, null);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot change a standard transaction into a transfer. Please create a new Transfer instead.");
  }

  @Test
  void shouldThrowIfAttemptToUpdateToTransferOutTypeTransactionUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Transaction transaction = new Transaction();
    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, null, TransactionType.TRANSFER_OUT, null, null, null, null);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot change a standard transaction into a transfer. Please create a new Transfer instead.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToUpdateTransaction() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, null);
    final UpdateTransactionCommand command = mock(UpdateTransactionCommand.class);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));
    when(command.userSid()).thenReturn(userSid);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(InsufficientPermissionsException.class)
        .hasMessage("User does not have the required role to update transactions for this account.");
  }

  @Test
  void shouldThrowIfCategoryIsNotFoundDuringTransactionUpdate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final Category category = CategoryUtils.createCategory("Gas", userSid, null, false, TransactionType.EXPENSE);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setCategory(category);
    final UUID categorySid = UUID.randomUUID();

    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, null, null, categorySid, null, null, null);

    when(transactionRepository.findBySid(any())).thenReturn(Optional.of(transaction));
    when(categoryService.fetchCategory(any())).thenThrow(new CategoryNotFoundException(categorySid));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(CategoryNotFoundException.class)
        .hasMessage("Category with SID [" + categorySid + "] was not found or does not belong to the user.");
  }

  @Test
  void shouldThrowIfTransactionIsAlreadyDeleted() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, transactionSid, null, null, BigDecimal.TEN, null, null);

    final Category mockCategory = new Category();
    mockCategory.setTransactionType(TransactionType.EXPENSE);

    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setAmount(BigDecimal.ONE);
    transaction.setStatus(TransactionStatus.DELETED);
    transaction.setCategory(mockCategory);
    transaction.setType(TransactionType.EXPENSE);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot update a deleted transaction.");
  }

  @Test
  void shouldThrowIfUpdatedCategoryTransactionTypeIsDifferentThanCurrentCategoryTransactionType() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final Category category = CategoryUtils.createCategory("Lunch", null, null, false, TransactionType.INCOME);
    final UpdateTransactionCommand command =
        new UpdateTransactionCommand(userSid, transactionSid, TransactionType.EXPENSE, category.getSid(),
            BigDecimal.TEN, null, null);

    final Category mockCategory = new Category();
    mockCategory.setTransactionType(TransactionType.INCOME);

    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setCategory(mockCategory);
    transaction.setAmount(BigDecimal.ONE);
    transaction.setStatus(TransactionStatus.ACTIVE);
    transaction.setType(TransactionType.EXPENSE);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));
    when(categoryService.fetchCategory(any())).thenReturn(category);

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(String.format("Category type [%s] does not match the requested transaction type [%s].",
            category.getTransactionType().name(), command.transactionType().name()));
  }

  @Test
  void shouldThrowIfUpdatedTransactionTypeDoesNotMatchPersistedCategoryType() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);

    final Category persistedCategory =
        CategoryUtils.createCategory("Salary", null, null, false, TransactionType.INCOME);
    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setCategory(persistedCategory);
    transaction.setAmount(BigDecimal.TEN);
    transaction.setStatus(TransactionStatus.ACTIVE);
    transaction.setType(TransactionType.INCOME);

    final UpdateTransactionCommand command = new UpdateTransactionCommand(
        userSid, transactionSid, TransactionType.EXPENSE, null, null, null, null
    );

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act & Assert
    assertThatThrownBy(() -> {
      service.updateTransaction(command);
    }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(String.format("Category type [%s] does not match the requested transaction type [%s].",
            persistedCategory.getTransactionType().name(), command.transactionType().name()));
  }

  @Test
  void shouldKeepPersistedFieldsIfNoUpdatedFieldWasSent() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID transactionSid = UUID.randomUUID();
    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    final Category persistedCategory =
        CategoryUtils.createCategory("Gas", userSid, null, false, TransactionType.EXPENSE);
    final BigDecimal persistedBalance = BigDecimal.TEN;
    final String persistedDescription = "Diesel for Car 1";
    final LocalDateTime persistedOccurreddAt = LocalDateTime.now().minusDays(5);
    final LocalDateTime persistedUpdatedAt = LocalDateTime.now().minusDays(10);

    final UpdateTransactionCommand command = new UpdateTransactionCommand(
        userSid,
        transactionSid,
        null,
        null,
        null,
        null,
        null
    );

    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setCategory(persistedCategory);
    transaction.setAmount(persistedBalance);
    transaction.setDescription(persistedDescription);
    transaction.setType(TransactionType.EXPENSE);
    transaction.setOccurredAt(persistedOccurreddAt);
    transaction.setUpdatedAt(persistedUpdatedAt);

    when(transactionRepository.findBySid(transactionSid)).thenReturn(Optional.of(transaction));

    // Act
    final Transaction updatedTransaction = service.updateTransaction(command);

    // Assert
    assertThat(updatedTransaction.getAmount()).isEqualByComparingTo(persistedBalance);
    assertThat(updatedTransaction.getCategory()).isEqualTo(persistedCategory);
    assertThat(updatedTransaction.getDescription()).isEqualTo(persistedDescription);
    assertThat(updatedTransaction.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(updatedTransaction.getOccurredAt()).isEqualTo(persistedOccurreddAt);
    assertThat(updatedTransaction.getUpdatedAt()).isEqualTo(persistedUpdatedAt);
  }

  @ParameterizedTest
  @MethodSource("updateTransactionPayload")
  void shouldUpdateTransactionAndAccountBalanceIfApplicable(
      final TransactionType persistedType,
      final BigDecimal persistedAmount,
      final UpdateTransactionCommand command,
      final BigDecimal expectedBalance,
      final BigDecimal expectedAmount,
      final TransactionType expectedType) {

    // Arrange
    final UUID userSid = command.userSid();

    final Account account = AccountUtils.createAccount(UUID.randomUUID(), userSid, MembershipRole.OWNER);
    account.setBalance(BigDecimal.valueOf(100));

    final Category persistedCategory = CategoryUtils.createCategory("Gas", userSid, null, false, expectedType);
    final String persistedDescription = "Diesel for Car 1";

    final Transaction persistedTransaction = createTransaction(
        account, persistedCategory, persistedAmount, persistedDescription, persistedType, LocalDateTime.now()
    );

    when(transactionRepository.findBySid(command.transactionSid())).thenReturn(Optional.of(persistedTransaction));

    if (command.categorySid() != null) {
      final Category newCategory = CategoryUtils.createCategory("New Category", userSid, null, false, expectedType);
      newCategory.setSid(command.categorySid());
      when(categoryService.fetchCategory(any())).thenReturn(newCategory);
    }

    // Act
    final Transaction updatedTransaction = service.updateTransaction(command);

    // Assert
    assertThat(updatedTransaction.getAmount()).isEqualByComparingTo(expectedAmount);
    assertThat(updatedTransaction.getType()).isEqualTo(expectedType);

    if (command.description() != null) {
      assertThat(updatedTransaction.getDescription()).isEqualTo(command.description());
    }

    assertThat(updatedTransaction.getAccount().getBalance()).isEqualByComparingTo(expectedBalance);
  }

  private Transaction createTransaction(final Account account, final Category category,
                                        final BigDecimal amount, final String description, final TransactionType type,
                                        final LocalDateTime time) {

    final Transaction transaction = new Transaction();
    transaction.setAccount(account);
    transaction.setCategory(category);
    transaction.setAmount(amount);
    transaction.setDescription(description);
    transaction.setType(type);
    transaction.setUpdatedAt(time);

    return transaction;
  }

}
