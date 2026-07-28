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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

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

  @Test
  void shouldThrowWhenUpdatingDetailsOnCanceledTransfer() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.CANCELED);
    // Act & Assert
    assertThatThrownBy(() -> rt.updateDetails("New desc", new BigDecimal("100"), true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot update a canceled recurring transfer.");
  }

  @Test
  void shouldUpdateDetailsSuccessfully() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    rt.updateDetails("Updated Insurance", new BigDecimal("75.00"), false);
    // Assert
    assertThat(rt.getDescription()).isEqualTo("Updated Insurance");
    assertThat(rt.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(rt.getAdjustToBusinessDay()).isFalse();
  }

  @Test
  void shouldThrowWhenAttemptingToSetStatusToCanceled() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act & Assert
    assertThatThrownBy(() -> rt.updateStatus(RecurringOperationStatus.CANCELED))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = RecurringOperationStatus.class, names = "ACTIVE")
  void shouldReturnFalseWhenIfNoOrEqualStatusIsGivenOnUpdate(final RecurringOperationStatus input) {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    // Act & Assert
    assertThat(rt.updateStatus(input)).isEqualTo(false);
  }

  @Test
  void shouldThrowIfNewStatusIsNotValidUpdate() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    // Act
    assertThatThrownBy(
        () -> assertThat(rt.updateStatus(RecurringOperationStatus.COMPLETED)))
        .isInstanceOf(IllegalStateException.class).hasMessage(
            "Recurring transfer with status [ACTIVE] cannot have its status manually updated to [COMPLETED].");
  }

  @Test
  void shouldThrowIfCurrentStatusIsTerminal() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.COMPLETED);
    // Act
    assertThatThrownBy(
        () -> assertThat(rt.updateStatus(RecurringOperationStatus.ACTIVE)))
        .isInstanceOf(IllegalStateException.class).hasMessage(
            "Recurring transfer with status [COMPLETED] cannot have its status manually updated to [ACTIVE].");
  }

  @Test
  void shouldUpdateStatusToPaused() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    // Act & Assert
    assertThat(rt.updateStatus(RecurringOperationStatus.PAUSED)).isEqualTo(true);
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.PAUSED);
  }

  @Test
  void shouldUpdateStatusToActive() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    rt.setStatus(RecurringOperationStatus.PAUSED);
    // Act & Assert
    assertThat(rt.updateStatus(RecurringOperationStatus.ACTIVE)).isEqualTo(true);
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.ACTIVE);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = Frequency.class, names = "MONTHLY")
  void shouldReturnFalseWhenIfNoOrEqualFrequencyIsGivenOnUpdate(final Frequency input) {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    // Act & Assert
    assertThat(rt.updateFrequency(input)).isEqualTo(false);
  }

  @Test
  void shouldUpdateFrequency() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2027, 1, 31), null);
    // Act & Assert
    assertThat(rt.updateFrequency(Frequency.BI_WEEKLY)).isEqualTo(true);
    assertThat(rt.getFrequency()).isEqualTo(Frequency.BI_WEEKLY);
  }

  @Test
  void shouldReturnFalseAndNotUpdateScheduleIfNoNewValuesAreGiven() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, LocalDate.of(2027, 1, 31), LocalDate.of(2028, 1, 31));
    // Act & Assert
    assertThat(rt.updateSchedule(null, null, null)).isEqualTo(false);
    assertThat(rt.getDayOfTheWeek()).isEqualTo(1);
    assertThat(rt.getDayOfTheMonth()).isEqualTo(24);
    assertThat(rt.getEndDate()).isEqualTo(LocalDate.of(2028, 1, 31));
  }

  @Test
  void shouldReturnTrueAndUpdateScheduleWhenAtLeastOneValueIsGiven() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, LocalDate.of(2027, 1, 31), LocalDate.of(2028, 1, 31));
    // Act & Assert
    assertThat(rt.updateSchedule(null, null, LocalDate.of(2029, 1, 31))).isEqualTo(true);
    assertThat(rt.getDayOfTheWeek()).isEqualTo(1);
    assertThat(rt.getDayOfTheMonth()).isEqualTo(24);
    assertThat(rt.getEndDate()).isEqualTo(LocalDate.of(2029, 1, 31));
  }

  @Test
  void shouldReturnTrueAndUpdateScheduleWhenAllValuesAreGiven() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, LocalDate.of(2027, 1, 31), LocalDate.of(2028, 1, 31));
    // Act & Assert
    assertThat(rt.updateSchedule(2, 25, LocalDate.of(2029, 1, 31))).isEqualTo(true);
    assertThat(rt.getDayOfTheWeek()).isEqualTo(2);
    assertThat(rt.getDayOfTheMonth()).isEqualTo(25);
    assertThat(rt.getEndDate()).isEqualTo(LocalDate.of(2029, 1, 31));
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


  @Test
  void shouldThrowIfSubmittedRequestedDateIsAfterCurrentEndDate() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, LocalDate.of(2027, 1, 31), LocalDate.of(2028, 1, 31));
    // Act
    assertThatThrownBy(
        () -> rt.updateNextExecutionDate(LocalDate.of(2029, 1, 1)))
        .isInstanceOf(IllegalStateException.class).hasMessage(
            "Requested execution date [2029-01-01] is after defined end date [2028-01-31].");
  }

  @Test
  void shouldUpdateExecutionDateWhenSubmittedValueIsValid() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, LocalDate.of(2027, 1, 31), LocalDate.of(2028, 1, 31));
    // Act
    rt.updateNextExecutionDate(LocalDate.of(2027, 1, 31));
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 1, 31));
  }

  @Test
  void shouldResolveExecutionDateIfNoValueIsSubmitted() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 24, null, LocalDate.of(2028, 1, 31));
    // Act
    rt.updateNextExecutionDate(null);
    // Assert
    assertThat(rt.getNextExecutionDate()).isNotNull();
  }

  @Test
  void shouldResolveExecutionDateWithoutValidationWhenEndDateIsNull() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 15, null, null);
    // Act
    rt.updateNextExecutionDate(null);
    // Assert
    assertThat(rt.getNextExecutionDate()).isNotNull();
  }

  @Test
  void shouldNotUpdateNextExecutionDateWhenStatusIsNotActive() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 15, LocalDate.of(2026, 8, 15), LocalDate.of(2028, 1, 31));
    rt.setStatus(RecurringOperationStatus.PAUSED);
    final LocalDate originalDate = rt.getNextExecutionDate();
    // Act
    rt.updateNextExecutionDate(null);
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(originalDate);
  }

  @Test
  void shouldNotUpdateNextExecutionDateWithExplicitValueWhenNotActive() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 15, LocalDate.of(2026, 8, 15), LocalDate.of(2028, 1, 31));
    rt.setStatus(RecurringOperationStatus.PAUSED);
    final LocalDate originalDate = rt.getNextExecutionDate();
    // Act
    rt.updateNextExecutionDate(LocalDate.of(2027, 1, 1));
    // Assert
    assertThat(rt.getNextExecutionDate()).isEqualTo(originalDate);
  }

  @Test
  void shouldThrowWhenAttemptingToCancelAlreadyCanceledRecurringTransfer() {
    // Arrange
    final RecurringTransfer rt = buildRecurringTransfer(LocalDate.of(2026, 7, 10), null);
    rt.setStatus(RecurringOperationStatus.CANCELED);

    // Act & Assert
    assertThatThrownBy(rt::markAsCanceled)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Recurring transfer is already canceled.");
  }

  @Test
  void shouldCancelRecurringTransfer() {
    // Arrange
    final RecurringTransfer rt =
        buildRecurringTransferWithSchedule(1, 15, LocalDate.of(2026, 8, 15), LocalDate.of(2028, 1, 31));
    final LocalDate originalEndDate = rt.getEndDate();
    // Act
    rt.markAsCanceled();
    // Assert
    assertThat(rt.getEndDate()).isNotEqualTo(originalEndDate);
    assertThat(rt.getStatus()).isEqualTo(RecurringOperationStatus.CANCELED);
  }


  private static RecurringTransfer buildRecurringTransferWithSchedule(final Integer dayOfWeek,
                                                                      final Integer dayOfMonth,
                                                                      final LocalDate nextExecutionDate,
                                                                      final LocalDate endDate) {
    final RecurringTransfer rt = new RecurringTransfer();
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setFrequency(Frequency.MONTHLY);
    rt.setDayOfTheWeek(dayOfWeek);
    rt.setDayOfTheMonth(dayOfMonth);
    rt.setNextExecutionDate(nextExecutionDate);
    rt.setEndDate(endDate);
    return rt;
  }

}
