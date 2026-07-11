package me.ferreira.graveto.moneytracker.transactions.recurringtransaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.scheduler.RecurringTransactionScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAndProcessRecurringTransactionIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransactionScheduler scheduler;
  @Autowired
  private RecurringTransactionRepository recurringTransactionRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private CategoryRepository categoryRepository;
  @Autowired
  private TransactionRepository transactionRepository;

  @Test
  void shouldCreateTransactionAndAdvanceNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();

    buildAndSaveRecurringTransaction(account, category, userSid, LocalDate.now(ZoneId.of("Europe/Lisbon")),
        Frequency.MONTHLY, false, null);

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    final Page<Transaction> transactions =
        transactionRepository.findAllByAccountId(account.getId(), Pageable.ofSize(10));

    assertThat(transactions.getTotalElements()).isEqualTo(1);

    final Transaction createdTransaction = transactions.getContent().stream()
        .filter(t -> t.getType() == TransactionType.EXPENSE)
        .findFirst()
        .orElseThrow();
    assertThat(createdTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(createdTransaction.getDescription()).isEqualTo("Home Insurance");

    final List<RecurringTransaction> updatedList =
        recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, LocalDate.now(ZoneId.of("Europe/Lisbon")).plusMonths(2));

    assertThat(updatedList).hasSize(1);
    assertThat(updatedList.getFirst().getLastExecutedAt()).isNotNull();
    assertThat(updatedList.getFirst().getNextExecutionDate())
        .isEqualTo(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusMonths(1));
  }

  @Test
  void shouldNotCreateTransactionWhenNothingIsDue() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();

    buildAndSaveRecurringTransaction(
        account, category, userSid, LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(5),
        Frequency.MONTHLY, false, null);

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    final Page<Transaction> transactions =
        transactionRepository.findAllByAccountId(account.getId(), Pageable.ofSize(10));
    assertThat(transactions.getTotalElements()).isEqualTo(0);
  }

  @Test
  void shouldMarkAsCompletedWhenPastEndDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account account = setupAccount(userSid);
    final Category category = setupCategory();

    final LocalDate today = LocalDate.now(ZoneId.of("Europe/Lisbon"));

    buildAndSaveRecurringTransaction(
        account, category, userSid, today, Frequency.MONTHLY, false, today.plusDays(15));

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    final List<RecurringTransaction> completedList =
        recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, today.plusMonths(2));

    assertThat(completedList).isEmpty();
  }

  private Account setupAccount(final UUID userSid) {
    final Account account = Account.create(BigDecimal.ZERO, Currency.EUR, "Santander");
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

  private Category setupCategory() {
    final List<Category> categories = categoryRepository.findAll();
    return categories.stream()
        .filter(c -> c.getTransactionType() == TransactionType.EXPENSE)
        .findFirst()
        .orElseThrow();
  }

  private void buildAndSaveRecurringTransaction(final Account account, final Category category,
                                                                final UUID userSid, final LocalDate nextExecutionDate,
                                                                final Frequency frequency,
                                                                final boolean adjustToBusinessDay,
                                                                final LocalDate endDate) {
    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(UUID.randomUUID());
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(userSid);
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setType(TransactionType.EXPENSE);
    rt.setFrequency(frequency);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setStartDate(nextExecutionDate);
    rt.setEndDate(endDate);
    recurringTransactionRepository.save(rt);
  }

}
