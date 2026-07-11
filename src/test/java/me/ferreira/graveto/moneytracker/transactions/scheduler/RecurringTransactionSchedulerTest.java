package me.ferreira.graveto.moneytracker.transactions.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecurringTransactionSchedulerTest {

  private final Clock fixedClock = Clock.fixed(
      LocalDate.of(2026, 7, 11).atStartOfDay(ZoneId.of("Europe/Lisbon")).toInstant(),
      ZoneId.of("Europe/Lisbon"));

  @InjectMocks
  private RecurringTransactionScheduler scheduler;
  @Mock
  private TransactionService transactionService;
  @Mock
  private RecurringTransactionRepository recurringTransactionRepository;

  @BeforeEach
  void setup() {
    scheduler = new RecurringTransactionScheduler(transactionService, recurringTransactionRepository, fixedClock);
  }

  @Test
  void shouldProcessActiveTransactionsDueToday() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);

    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    verify(transactionService).createTransaction(any(CreateTransactionCommand.class));
    verify(recurringTransactionRepository).saveAll(List.of(rt));
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(rt.getLastExecutedAt()).isNotNull();
  }

  @Test
  void shouldSkipWeekendWhenAdjustToBusinessDayIsTrue() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, true);

    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    verify(transactionService, never()).createTransaction(any());
    verify(recurringTransactionRepository).saveAll(List.of());
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 11));
  }

  @Test
  void shouldExecuteOnWeekendWhenAdjustToBusinessDayIsFalse() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, false);

    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    verify(transactionService).createTransaction(any(CreateTransactionCommand.class));
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 11));
  }

  @Test
  void shouldNotProcessOrSaveWhenListIsEmpty() {
    // Arrange
    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of());

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    verify(transactionService, never()).createTransaction(any());
    verify(recurringTransactionRepository, never()).saveAll(any());
  }

  @Test
  void shouldContinueProcessingWhenOneTransactionFails() {
    // Arrange
    final RecurringTransaction failingRt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);
    final RecurringTransaction successRt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);

    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(failingRt, successRt));
    when(transactionService.createTransaction(any()))
        .thenThrow(new RuntimeException("Account closed"))
        .thenReturn(null);

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    verify(transactionService, times(2)).createTransaction(any());

    final ArgumentCaptor<List<RecurringTransaction>> captor = ArgumentCaptor.forClass(List.class);
    verify(recurringTransactionRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().getFirst()).isEqualTo(successRt);
  }

  @Test
  void shouldOnlySaveProcessedTransactions() {
    // Arrange
    final RecurringTransaction skippedRt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, true);
    final RecurringTransaction processedRt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, false);

    when(recurringTransactionRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(skippedRt, processedRt));

    // Act
    scheduler.registerRecurringTransaction();

    // Assert
    final ArgumentCaptor<List<RecurringTransaction>> captor = ArgumentCaptor.forClass(List.class);
    verify(recurringTransactionRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().getFirst()).isEqualTo(processedRt);
  }

  private static RecurringTransaction buildRecurringTransaction(final LocalDate nextExecutionDate,
                                                                final Frequency frequency,
                                                                final boolean adjustToBusinessDay) {
    final Account account = new Account();
    account.setSid(UUID.randomUUID());

    final Category category = new Category();
    category.setSid(UUID.randomUUID());

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(UUID.randomUUID());
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(UUID.randomUUID());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setType(TransactionType.EXPENSE);
    rt.setFrequency(frequency);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    return rt;
  }

}
