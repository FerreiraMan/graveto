package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import org.junit.jupiter.api.Test;

public class RecurringTransactionTest {

  @Test
  void shouldAdvanceNextExecutionDateByDays() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.DAYS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 11));
  }

  @Test
  void shouldAdvanceNextExecutionDateByWeeks() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.WEEKS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 17));
  }

  @Test
  void shouldAdvanceNextExecutionDateByBiWeekly() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);

    // Act
    rt.updateNextExecutionDate(2L, ChronoUnit.WEEKS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 24));
  }

  @Test
  void shouldAdvanceNextExecutionDateByMonths() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.MONTHS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
  }

  @Test
  void shouldAdvanceNextExecutionDateByYears() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.YEARS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 7, 10));
  }

  @Test
  void shouldSetLastExecutedAtOnUpdate() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);
    assertThat(rt.getLastExecutedAt()).isNull();

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.DAYS);

    // Assert
    assertThat(rt.getLastExecutedAt()).isNotNull();
  }

  @Test
  void shouldSetStatusToCompletedWhenNextExecutionDatePassesEndDate() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15));
    rt.setStatus(RecurringOperationStatus.ACTIVE);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.WEEKS);

    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.COMPLETED);
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 17));
  }

  @Test
  void shouldNotCompleteWhenNextExecutionDateIsBeforeEndDate() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(
        LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20));
    rt.setStatus(RecurringOperationStatus.ACTIVE);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.WEEKS);

    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldNotCompleteWhenEndDateIsNull() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.ACTIVE);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.MONTHS);

    // Assert
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 8, 10));
  }

  @Test
  void shouldHandleMonthEndEdgeCaseForMonthlyFrequency() {
    // Arrange
    final RecurringTransaction rt = buildRecurringTransaction(LocalDate.of(2027, 1, 31), null);

    // Act
    rt.updateNextExecutionDate(1L, ChronoUnit.MONTHS);

    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 2, 28));
  }

  private static RecurringTransaction buildRecurringTransaction(final LocalDate nextExecutionDate,
                                                                final LocalDate endDate) {
    final RecurringTransaction rt = new RecurringTransaction();
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setEndDate(endDate);
    return rt;
  }

}
