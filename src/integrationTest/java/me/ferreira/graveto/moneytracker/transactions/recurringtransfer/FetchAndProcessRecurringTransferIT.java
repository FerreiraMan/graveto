package me.ferreira.graveto.moneytracker.transactions.recurringtransfer;

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
import me.ferreira.graveto.moneytracker.config.MoneyTrackerBaseIntegrationTest;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.scheduler.RecurringTransferScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/moneytracker/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FetchAndProcessRecurringTransferIT extends MoneyTrackerBaseIntegrationTest {

  @Autowired
  private RecurringTransferScheduler scheduler;
  @Autowired
  private RecurringTransferRepository recurringTransferRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private TransactionRepository transactionRepository;

  @Test
  void shouldCreateTransferAndAdvanceNextExecutionDate() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupSourceAccount(userSid);
    final Account destinationAccount = setupDestinationAccount(userSid);

    buildAndSaveRecurringTransfer(sourceAccount, destinationAccount, userSid, LocalDate.now(ZoneId.of("Europe/Lisbon")),
        Frequency.MONTHLY, false, null);

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    final Page<Transaction> sourceTransaction =
        transactionRepository.findAllByAccountId(sourceAccount.getId(), Pageable.ofSize(10));
    final Page<Transaction> destinationTransaction =
        transactionRepository.findAllByAccountId(destinationAccount.getId(), Pageable.ofSize(10));

    assertThat(sourceTransaction.getTotalElements()).isEqualTo(1);
    assertThat(destinationTransaction.getTotalElements()).isEqualTo(1);

    final List<RecurringTransfer> updatedList =
        recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, LocalDate.now(ZoneId.of("Europe/Lisbon")).plusMonths(2));

    assertThat(updatedList).hasSize(1);
    assertThat(updatedList.getFirst().getLastExecutedAt()).isNotNull();
    assertThat(updatedList.getFirst().getNextExecutionDate())
        .isEqualTo(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusMonths(1));
  }

  @Test
  void shouldNotCreateTransferWhenNothingIsDue() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Account sourceAccount = setupSourceAccount(userSid);
    final Account destinationAccount = setupDestinationAccount(userSid);
    final LocalDate today = LocalDate.now(ZoneId.of("Europe/Lisbon"));

    buildAndSaveRecurringTransfer(
        sourceAccount, destinationAccount, userSid, today.plusDays(5),
        Frequency.MONTHLY, false, null);

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    final List<RecurringTransfer> completedList =
        recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
            RecurringOperationStatus.ACTIVE, today);

    assertThat(completedList).isEmpty();

    final Page<Transaction> sourceTransaction =
        transactionRepository.findAllByAccountId(sourceAccount.getId(), Pageable.ofSize(10));
    final Page<Transaction> destinationTransaction =
        transactionRepository.findAllByAccountId(destinationAccount.getId(), Pageable.ofSize(10));
    assertThat(sourceTransaction.getTotalElements()).isEqualTo(0);
    assertThat(destinationTransaction.getTotalElements()).isEqualTo(0);
  }

  private Account setupSourceAccount(final UUID userSid) {
    final Account account = Account.create(BigDecimal.TEN, Currency.EUR, "Santander");
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

  private Account setupDestinationAccount(final UUID userSid) {
    final Account account = Account.create(BigDecimal.ONE, Currency.EUR, "BCP");
    account.addMembership(AccountMembership.create(userSid, MembershipRole.OWNER));
    return accountRepository.save(account);
  }

  private void buildAndSaveRecurringTransfer(final Account sourceAccount, final Account destinationAccount,
                                             final UUID userSid, final LocalDate nextExecutionDate,
                                             final Frequency frequency,
                                             final boolean adjustToBusinessDay,
                                             final LocalDate endDate) {
    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(UUID.randomUUID());
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setUserSid(userSid);
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setFrequency(frequency);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setStartDate(nextExecutionDate);
    rt.setEndDate(endDate);
    recurringTransferRepository.save(rt);
  }

}
