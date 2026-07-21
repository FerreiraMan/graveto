package me.ferreira.graveto.moneytracker.transactions.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.service.command.transfer.CreateTransferCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecurringTransferSchedulerTest {

  private final Clock fixedClock = Clock.fixed(
      LocalDate.of(2026, 7, 11).atStartOfDay(ZoneId.of("Europe/Lisbon")).toInstant(),
      ZoneId.of("Europe/Lisbon"));

  @InjectMocks
  private RecurringTransferScheduler scheduler;
  @Mock
  private TransferService transferService;
  @Mock
  private RecurringTransferRepository recurringTransferRepository;

  @BeforeEach
  void setup() {
    scheduler = new RecurringTransferScheduler(transferService, recurringTransferRepository, fixedClock);
  }

  @Test
  void shouldProcessActiveTransfersDueToday() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);

    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    verify(transferService).createTransfer(any(CreateTransferCommand.class));
    verify(recurringTransferRepository).saveAll(List.of(rt));
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(rt.getLastExecutedAt()).isNotNull();
  }

  @Test
  void shouldSkipWeekendWhenAdjustToBusinessDayIsTrue() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, true);

    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    verify(transferService, never()).createTransfer(any());
    verify(recurringTransferRepository).saveAll(List.of());
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 11));
  }

  @Test
  void shouldExecuteOnWeekendWhenAdjustToBusinessDayIsFalse() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, false);

    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(rt));

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    verify(transferService).createTransfer(any(CreateTransferCommand.class));
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 11));
  }

  @Test
  void shouldNotProcessOrSaveWhenListIsEmpty() {
    // Arrange
    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of());

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    verify(transferService, never()).createTransfer(any());
    verify(recurringTransferRepository, never()).saveAll(any());
  }

  @Test
  void shouldContinueProcessingWhenOneTransferFails() {
    // Arrange
    final RecurringTransfer failingRt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);
    final RecurringTransfer successRt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 10), Frequency.MONTHLY, false);

    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(failingRt, successRt));
    when(transferService.createTransfer(any()))
        .thenThrow(new RuntimeException("Account closed"))
        .thenReturn(null);

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    verify(transferService, times(2)).createTransfer(any());

    final ArgumentCaptor<List<RecurringTransfer>> captor = ArgumentCaptor.forClass(List.class);
    verify(recurringTransferRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().getFirst()).isEqualTo(successRt);
  }

  @Test
  void shouldOnlySaveProcessedTransfers() {
    // Arrange
    final RecurringTransfer skippedRt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, true);
    final RecurringTransfer processedRt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 11), Frequency.MONTHLY, false);

    when(recurringTransferRepository.findAllByStatusAndNextExecutionDateLessThanEqual(
        any(), any())).thenReturn(List.of(skippedRt, processedRt));

    // Act
    scheduler.registerRecurringTransfer();

    // Assert
    final ArgumentCaptor<List<RecurringTransfer>> captor = ArgumentCaptor.forClass(List.class);
    verify(recurringTransferRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().getFirst()).isEqualTo(processedRt);
  }

  private static RecurringTransfer buildRecurringTransfer(final LocalDate nextExecutionDate,
                                                          final Frequency frequency,
                                                          final boolean adjustToBusinessDay) {
    final Account sourceAccount = new Account();
    sourceAccount.setSid(UUID.randomUUID());

    final Account destinationAccount = new Account();
    destinationAccount.setSid(UUID.randomUUID());

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(UUID.randomUUID());
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setUserSid(UUID.randomUUID());
    rt.setDescription("Home Insurance");
    rt.setAmount(new BigDecimal("50.00"));
    rt.setCurrency(Currency.EUR);
    rt.setFrequency(frequency);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    return rt;
  }

}
