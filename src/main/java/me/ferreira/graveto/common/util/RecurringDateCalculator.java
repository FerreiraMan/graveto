package me.ferreira.graveto.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import me.ferreira.graveto.common.domain.Frequency;

public final class RecurringDateCalculator {

  private RecurringDateCalculator() {
  }

  public static LocalDate calculateNextExecution(final Frequency frequency, final Integer dayOfWeek,
                                                 final Integer dayOfTheMonth) {
    final LocalDate today = LocalDate.now();

    return switch (frequency) {
      case DAILY -> today.plusDays(1);
      case WEEKLY -> dayOfWeek <= today.getDayOfWeek().getValue()
          ? today.plusWeeks(1).with(ChronoField.DAY_OF_WEEK, dayOfWeek)
          : today.with(ChronoField.DAY_OF_WEEK, dayOfWeek);
      case BI_WEEKLY -> dayOfWeek <= today.getDayOfWeek().getValue()
          ? today.plusWeeks(2).with(ChronoField.DAY_OF_WEEK, dayOfWeek)
          : today.with(ChronoField.DAY_OF_WEEK, dayOfWeek);
      case MONTHLY -> {
        final int targetMonth = dayOfTheMonth > today.getDayOfMonth()
            ? today.getMonthValue() : today.plusMonths(1).getMonthValue();
        final boolean isNextYear = dayOfTheMonth <= today.getDayOfMonth() && targetMonth == 1;
        final int targetYear = isNextYear ? today.plusYears(1).getYear() : today.getYear();

        final YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        final boolean isValidDayInMonth = yearMonth.isValidDay(dayOfTheMonth);

        yield LocalDate.of(targetYear, targetMonth,
            isValidDayInMonth ? dayOfTheMonth : yearMonth.atEndOfMonth().getDayOfMonth());
      }
      case ANNUALLY -> dayOfTheMonth <= today.getDayOfMonth()
          ? today.plusYears(1).withDayOfMonth(dayOfTheMonth)
          : today.withDayOfMonth(dayOfTheMonth);
    };
  }

}
