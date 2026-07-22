package me.ferreira.graveto.common.util;

import java.time.LocalDate;
import me.ferreira.graveto.common.domain.Frequency;

public final class TemporalConfigValidator {

  private TemporalConfigValidator() {
  }

  public static void validateTemporalInputs(final LocalDate startDate, final LocalDate endDate) {

    if (endDate != null && startDate != null
        && endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("End date must be after start date.");
    }
  }

  public static void validateFrequencyAndDayConfig(final Frequency frequency, final Integer dayOfWeek,
                                                    final Integer dayOfMonth) {

    if (Frequency.MONTHLY.equals(frequency) && dayOfMonth == null) {
      throw new IllegalArgumentException("Day of the month needs to be provided when selecting monthly operation.");
    }
    if ((Frequency.WEEKLY.equals(frequency) || Frequency.BI_WEEKLY.equals(frequency)) && dayOfWeek == null) {
      throw new IllegalArgumentException(
          "Day of the week needs to be provided when selecting weekly or bi-weekly operation.");
    }
    if (Frequency.ANNUALLY.equals(frequency) && dayOfMonth == null) {
      throw new IllegalArgumentException("Day of the month needs to be provided when selecting annual operation.");
    }
  }

  public static void validateExecutionDate(final LocalDate nextExecutionDate, final LocalDate endDate) {
    if (nextExecutionDate != null && endDate != null && nextExecutionDate.isAfter(endDate)) {
      throw new IllegalStateException(
          "Requested execution date [" + nextExecutionDate + "] is after defined end date [" + endDate + "].");
    }
  }

}