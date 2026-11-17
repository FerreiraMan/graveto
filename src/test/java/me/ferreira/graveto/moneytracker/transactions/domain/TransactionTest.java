package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import org.junit.jupiter.api.Test;

public class TransactionTest {

  @Test
  void shouldCreateOpeningTransactionWithGeneratedSid() {
    // Arrange
    final BigDecimal amount = new BigDecimal("1000.50");
    final Currency currency = Currency.EUR;
    final String institution = "Santander";
    final Account account = Account.create(amount, currency, institution);
    final Category validCategory = CategoryUtils.createInitialBalanceCategory();

    // Act
    final Transaction transaction = Transaction.createOpeningTransaction(account, amount, validCategory);

    // Assert
    assertThat(transaction.getSid()).isNotNull();
    assertThat(transaction.getAccount().getSid()).isEqualTo(account.getSid());
    assertThat(transaction.getAmount()).isEqualTo(account.getBalance());
    assertThat(transaction.getCategory()).isEqualTo(validCategory);
    assertThat(transaction.getCurrency()).isEqualTo(account.getBaseCurrency());
    assertThat(transaction.getType()).isEqualTo(TransactionType.OPENING_BALANCE);
    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
  }

  @Test
  void shouldCreateTransactionWithGeneratedSid() {
    // Arrange
    final BigDecimal amount = new BigDecimal("1000.50");
    final Currency currency = Currency.EUR;
    final String description = "Lunch";
    final String institution = "Santander";
    final Account account = Account.create(amount, currency, institution);
    final Category validCategory = CategoryUtils.createInitialBalanceCategory();
    final LocalDateTime occurredAt = LocalDateTime.now();

    // Act
    final Transaction transaction =
        Transaction.create(account, amount, description, validCategory, TransactionType.EXPENSE, occurredAt);

    // Assert
    assertThat(transaction.getSid()).isNotNull();
    assertThat(transaction.getAccount().getSid()).isEqualTo(account.getSid());
    assertThat(transaction.getAmount()).isEqualTo(account.getBalance());
    assertThat(transaction.getCategory()).isEqualTo(validCategory);
    assertThat(transaction.getCurrency()).isEqualTo(account.getBaseCurrency());
    assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
    assertThat(transaction.getOccurredAt()).isEqualTo(occurredAt);
  }

  @Test
  void shouldCreateTransactionWithCorrelationId() {
    // Arrange
    final BigDecimal amount = new BigDecimal("1000.50");
    final Currency currency = Currency.EUR;
    final String description = "Lunch";
    final String institution = "Santander";
    final Account account = Account.create(amount, currency, institution);
    final Category validCategory = CategoryUtils.createInitialBalanceCategory();
    final LocalDateTime occurredAt = LocalDateTime.now();

    final UUID correlationId = UUID.randomUUID();

    // Act
    final Transaction transaction =
        Transaction.createTransferTransaction(account, amount, description, correlationId, validCategory,
            TransactionType.EXPENSE, occurredAt);

    // Assert
    assertThat(transaction.getSid()).isNotNull();
    assertThat(transaction.getAccount().getSid()).isEqualTo(account.getSid());
    assertThat(transaction.getAmount()).isEqualTo(account.getBalance());
    assertThat(transaction.getCorrelationId()).isEqualTo(correlationId);
    assertThat(transaction.getCategory()).isEqualTo(validCategory);
    assertThat(transaction.getCurrency()).isEqualTo(account.getBaseCurrency());
    assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
    assertThat(transaction.getOccurredAt()).isEqualTo(occurredAt);
  }

  @Test
  void shouldStartWithoutGroup() {
    // Arrange & Act
    final Transaction transaction = buildTransaction();

    // Assert
    assertThat(transaction.getTransactionGroup()).isNull();
  }

  @Test
  void shouldAddTransactionToGroupWhenItHasNone() {
    // Arrange
    final Transaction transaction = buildTransaction();
    final TransactionGroup target = TransactionGroup.create("Algarve 2026", UUID.randomUUID());

    // Act
    final GroupAssignmentOutcome outcome = transaction.assignToGroup(target);

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.ADDED);
    assertThat(transaction.getTransactionGroup()).isSameAs(target);
  }

  @Test
  void shouldMoveTransactionWhenItIsInAnotherGroup() {
    // Arrange
    final Transaction transaction = buildTransaction();
    final TransactionGroup current = TransactionGroup.create("Birthday", UUID.randomUUID());
    final TransactionGroup target = TransactionGroup.create("Algarve 2026", UUID.randomUUID());
    transaction.assignToGroup(current);

    // Act
    final GroupAssignmentOutcome outcome = transaction.assignToGroup(target);

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.MOVED);
    assertThat(transaction.getTransactionGroup()).isSameAs(target);
  }

  @Test
  void shouldLeaveTransactionUnchangedWhenItIsAlreadyInTheTargetGroup() {
    // Arrange
    final Transaction transaction = buildTransaction();
    final TransactionGroup target = TransactionGroup.create("Algarve 2026", UUID.randomUUID());
    transaction.assignToGroup(target);

    // Act
    final GroupAssignmentOutcome outcome = transaction.assignToGroup(target);

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.UNCHANGED);
    assertThat(transaction.getTransactionGroup()).isSameAs(target);
  }

  @Test
  void shouldTreatGroupsWithTheSameSidAsTheSameGroup() {
    // Arrange
    final Transaction transaction = buildTransaction();
    final TransactionGroup current = TransactionGroup.create("Algarve 2026", UUID.randomUUID());
    final TransactionGroup sameSidDifferentInstance = TransactionGroup.create("Algarve 2026", UUID.randomUUID());
    sameSidDifferentInstance.setSid(current.getSid());
    transaction.assignToGroup(current);

    // Act
    final GroupAssignmentOutcome outcome = transaction.assignToGroup(sameSidDifferentInstance);

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.UNCHANGED);
    assertThat(transaction.getTransactionGroup()).isSameAs(current);
  }

  @Test
  void shouldRemoveTransactionFromItsGroup() {
    // Arrange
    final Transaction transaction = buildTransaction();
    transaction.assignToGroup(TransactionGroup.create("Algarve 2026", UUID.randomUUID()));

    // Act
    final GroupAssignmentOutcome outcome = transaction.removeFromGroup();

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.REMOVED);
    assertThat(transaction.getTransactionGroup()).isNull();
  }

  @Test
  void shouldLeaveTransactionUnchangedWhenRemovingFromGroupItDoesNotHave() {
    // Arrange
    final Transaction transaction = buildTransaction();

    // Act
    final GroupAssignmentOutcome outcome = transaction.removeFromGroup();

    // Assert
    assertThat(outcome).isEqualTo(GroupAssignmentOutcome.UNCHANGED);
    assertThat(transaction.getTransactionGroup()).isNull();
  }

  private Transaction buildTransaction() {
    final BigDecimal amount = new BigDecimal("1000.50");
    final Account account = Account.create(amount, Currency.EUR, "Santander");
    final Category category = CategoryUtils.createInitialBalanceCategory();

    return Transaction.create(account, amount, "Lunch", category, TransactionType.EXPENSE, LocalDateTime.now());
  }

}
