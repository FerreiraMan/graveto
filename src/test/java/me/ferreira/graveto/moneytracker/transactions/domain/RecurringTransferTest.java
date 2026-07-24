package me.ferreira.graveto.moneytracker.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;
import org.junit.jupiter.api.Test;

public class RecurringTransferTest {

  @Test
  void shouldCreateRecurringTransferWithGeneratedSid() {
    // Arrange
    final Account sourceAccount = new Account();
    sourceAccount.setSid(UUID.randomUUID());
    sourceAccount.setBaseCurrency(Currency.EUR);
    sourceAccount.setStatus(AccountStatus.ACTIVE);

    final Account destinationAccount = new Account();
    destinationAccount.setSid(UUID.randomUUID());
    destinationAccount.setBaseCurrency(Currency.EUR);
    destinationAccount.setStatus(AccountStatus.ACTIVE);

    final UUID userSid = UUID.randomUUID();
    final LocalDate startDate = LocalDate.of(2026, 8, 15);
    final LocalDate endDate = LocalDate.of(2027, 8, 15);

    // Act
    final RecurringTransfer rt = RecurringTransfer.create(
        sourceAccount, destinationAccount, userSid, "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null, true, startDate, endDate);

    // Assert
    assertThat(rt.getSid()).isNotNull();
    assertThat(rt.getSourceAccount()).isEqualTo(sourceAccount);
    assertThat(rt.getDestinationAccount()).isEqualTo(destinationAccount);
    assertThat(rt.getUserSid()).isEqualTo(userSid);
    assertThat(rt.getDescription()).isEqualTo("Home Insurance");
    assertThat(rt.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(rt.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(rt.getFrequency()).isEqualTo(Frequency.MONTHLY);
    assertThat(rt.getDayOfTheMonth()).isEqualTo(15);
    assertThat(rt.getDayOfTheWeek()).isNull();
    assertThat(rt.getAdjustToBusinessDay()).isTrue();
    assertThat(rt.getNextExecutionDate()).isEqualTo(startDate);
    assertThat(rt.getStartDate()).isEqualTo(startDate);
    assertThat(rt.getEndDate()).isEqualTo(endDate);
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
    assertThat(rt.getLastExecutedAt()).isNull();
  }

  @Test
  void shouldCreateRecurringTransferWithNullEndDate() {
    // Arrange
    final Account sourceAccount = new Account();
    final Account destinationAccount = new Account();
    final LocalDate startDate = LocalDate.of(2026, 8, 1);

    // Act
    final RecurringTransfer rt = RecurringTransfer.create(
        sourceAccount, destinationAccount, UUID.randomUUID(), "Home Insurance", new BigDecimal("50.00"),
        Frequency.MONTHLY, 15, null, true, startDate, null);

    // Assert
    assertThat(rt.getEndDate()).isNull();
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @Test
  void shouldCreateWeeklyRecurringTransferWithDayOfWeek() {
    // Arrange
    final Account sourceAccount = new Account();
    final Account destinationAccount = new Account();
    final LocalDate startDate = LocalDate.of(2026, 8, 1);

    // Act
    final RecurringTransfer rt = RecurringTransfer.create(
        sourceAccount, destinationAccount, UUID.randomUUID(), "Home Insurance", new BigDecimal("50.00"),
        Frequency.WEEKLY, null, 1, false, startDate, null);

    // Assert
    assertThat(rt.getFrequency()).isEqualTo(Frequency.WEEKLY);
    assertThat(rt.getDayOfTheWeek()).isEqualTo(1);
    assertThat(rt.getDayOfTheMonth()).isNull();
    assertThat(rt.getAdjustToBusinessDay()).isFalse();
  }

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
