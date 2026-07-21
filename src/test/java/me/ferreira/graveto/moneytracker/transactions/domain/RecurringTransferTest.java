package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import org.junit.jupiter.api.Test;

public class RecurringTransferTest {

  @Test
  void shouldThrowIfSchedulerExecutionDateOnNonActiveRecurringTransfer() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.PAUSED);
    // Act
    assertThatThrownBy(
        () -> rt.scheduleNextExecutionDate(1L, ChronoUnit.DAYS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Scheduled operation is not in an active state.");
  }

  @Test
  void shouldAdvanceNextExecutionDateByDays() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.DAYS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 11));
  }

  @Test
  void shouldAdvanceNextExecutionDateByWeeks() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.WEEKS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 17));
  }

  @Test
  void shouldAdvanceNextExecutionDateByBiWeekly() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.scheduleNextExecutionDate(2L, ChronoUnit.WEEKS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 24));
  }

  @Test
  void shouldAdvanceNextExecutionDateByMonths() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.MONTHS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
  }

  @Test
  void shouldAdvanceNextExecutionDateByYears() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.YEARS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 7, 10));
  }

  @Test
  void shouldSetLastExecutedAtOnUpdate() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    assertThat(rt.getLastExecutedAt()).isNull();
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.DAYS);
    // Assert
    assertThat(rt.getLastExecutedAt()).isNotNull();
  }

  @Test
  void shouldSetStatusToCompletedWhenNextExecutionDatePassesEndDate() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.WEEKS);
    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.COMPLETED);
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 17));
  }

  @Test
  void shouldNotCompleteWhenNextExecutionDateIsBeforeEndDate() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(
        LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20));
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.WEEKS);
    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldNotCompleteWhenEndDateIsNull() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.MONTHS);
    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
  }

  @Test
  void shouldHandleMonthEndEdgeCaseForMonthlyFrequency() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    // Act
    rt.scheduleNextExecutionDate(1L, ChronoUnit.MONTHS);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 2, 28));
  }

  private static RecurringTransfer buildRecurringTransfer(final LocalDate nextExecutionDate,
                                                          final LocalDate endDate) {
    final RecurringTransfer rt = new RecurringTransfer();
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setEndDate(endDate);
    return rt;
  }

}
