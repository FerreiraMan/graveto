package me.ferreira.graveto.moneytracker.transactions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recurring_transfers")
public class RecurringTransfer extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recurring_transfers_id_seq")
  @SequenceGenerator(
      name = "recurring_transfers_id_seq", sequenceName = "recurring_transfers_id_seq", allocationSize = 1
  )
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @JoinColumn(name = "source_account_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Account sourceAccount;

  @JoinColumn(name = "destination_account_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Account destinationAccount;

  @Column(name = "user_sid", nullable = false)
  private UUID userSid;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Frequency frequency;

  @Column(name = "day_of_month")
  private Integer dayOfTheMonth;

  @Column(name = "day_of_week")
  private Integer dayOfTheWeek;

  @Column(name = "adjust_to_business_day")
  private Boolean adjustToBusinessDay = true;

  @Column(name = "next_execution_date", nullable = false)
  private LocalDate nextExecutionDate;

  @Column(name = "last_executed_at")
  private LocalDateTime lastExecutedAt;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private RecurringOperationStatus status;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  public static RecurringTransfer create(final Account sourceAccount, final Account destinationAccount,
                                         final UUID userSid,
                                         final String description, final BigDecimal amount,
                                         final Frequency frequency, final Integer dayOfTheMonth,
                                         final Integer dayOfTheWeek, final Boolean adjustToBusinessDay,
                                         final LocalDate startDate, final LocalDate endDate) {

    final RecurringTransfer rt = new RecurringTransfer();
    rt.setSid(UUID.randomUUID());
    rt.setSourceAccount(sourceAccount);
    rt.setDestinationAccount(destinationAccount);
    rt.setUserSid(userSid);
    rt.setDescription(description);
    rt.setAmount(amount);
    rt.setCurrency(sourceAccount.getBaseCurrency());
    rt.setFrequency(frequency);
    rt.setDayOfTheMonth(dayOfTheMonth);
    rt.setDayOfTheWeek(dayOfTheWeek);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(endDate);

    final LocalDate effectiveStartDate =
        startDate != null ? startDate : rt.resolveExecutionDate(dayOfTheWeek, dayOfTheMonth);

    rt.setNextExecutionDate(effectiveStartDate);
    rt.setStartDate(effectiveStartDate);

    return rt;
  }

  public void scheduleNextExecutionDate(final Long amount, final ChronoUnit temporalUnit) {

    if (!RecurringOperationStatus.ACTIVE.equals(this.status)) {
      throw new IllegalStateException("Scheduled operation is not in an active state.");
    }

    this.lastExecutedAt = LocalDateTime.now();
    this.nextExecutionDate = this.nextExecutionDate.plus(amount, temporalUnit);

    if (this.endDate != null && this.nextExecutionDate.isAfter(endDate)) {
      this.status = RecurringOperationStatus.COMPLETED;
    }
  }

  private LocalDate resolveExecutionDate(final Integer dayOfWeek, final Integer dayOfTheMonth) {

    final LocalDate today = LocalDate.now();

    return switch (this.frequency) {
      case DAILY -> today.plusDays(1);
      case WEEKLY -> dayOfWeek <= today.getDayOfWeek().getValue()
          ? today.plusWeeks(1).with(ChronoField.DAY_OF_WEEK, dayOfWeek) :
          today.with(ChronoField.DAY_OF_WEEK, dayOfWeek);
      case BI_WEEKLY -> dayOfWeek <= today.getDayOfWeek().getValue()
          ? today.plusWeeks(2).with(ChronoField.DAY_OF_WEEK, dayOfWeek) :
          today.with(ChronoField.DAY_OF_WEEK, dayOfWeek);
      case MONTHLY -> {
        final int targetMonth =
            dayOfTheMonth > today.getDayOfMonth() ? today.getMonthValue() : today.plusMonths(1).getMonthValue();
        final boolean isNextYear = dayOfTheMonth <= today.getDayOfMonth() && targetMonth == 1;
        final int targetYear = isNextYear ? today.plusYears(1).getYear() : today.getYear();

        final YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        final boolean isValidDayInMonth = yearMonth.isValidDay(dayOfTheMonth);

        yield LocalDate.of(targetYear, targetMonth,
            isValidDayInMonth ? dayOfTheMonth : yearMonth.atEndOfMonth().getDayOfMonth());
      }
      case ANNUALLY -> dayOfTheMonth <= today.getDayOfMonth()
          ? today.plusYears(1).withDayOfMonth(dayOfTheMonth) :
          today.withDayOfMonth(dayOfTheMonth);
    };
  }

}
