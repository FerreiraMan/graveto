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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.domain.Frequency;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.common.util.RecurringDateCalculator;
import me.ferreira.graveto.common.util.TemporalConfigValidator;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recurring_transactions_id_seq")
  @SequenceGenerator(
      name = "recurring_transactions_id_seq", sequenceName = "recurring_transactions_id_seq", allocationSize = 1
  )
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID sid;

  @JoinColumn(name = "account_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Account account;

  @JoinColumn(name = "category_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Category category;

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
  private TransactionType type;

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

  public static RecurringTransaction create(final Account account, final Category category, final UUID userSid,
                                            final String description, final BigDecimal amount,
                                            final TransactionType transactionType,
                                            final Frequency frequency, final Integer dayOfTheMonth,
                                            final Integer dayOfTheWeek, final Boolean adjustToBusinessDay,
                                            final LocalDate startDate, final LocalDate endDate) {

    final RecurringTransaction rt = new RecurringTransaction();
    rt.setSid(UUID.randomUUID());
    rt.setAccount(account);
    rt.setCategory(category);
    rt.setUserSid(userSid);
    rt.setDescription(description);
    rt.setAmount(amount);
    rt.setCurrency(account.getBaseCurrency());
    rt.setType(transactionType);
    rt.setFrequency(frequency);
    rt.setDayOfTheMonth(dayOfTheMonth);
    rt.setDayOfTheWeek(dayOfTheWeek);
    rt.setAdjustToBusinessDay(adjustToBusinessDay);
    rt.setStatus(RecurringOperationStatus.ACTIVE);
    rt.setEndDate(endDate);

    final LocalDate effectiveStartDate =
        startDate != null ? startDate :
            RecurringDateCalculator.calculateNextExecution(frequency, dayOfTheWeek, dayOfTheMonth);

    rt.setNextExecutionDate(effectiveStartDate);
    rt.setStartDate(effectiveStartDate);

    return rt;
  }

  public void updateDetails(final String description, final BigDecimal amount, final Boolean adjustToBusinessDay) {

    if (this.status == RecurringOperationStatus.CANCELED) {
      throw new IllegalStateException("Cannot update a canceled recurring transaction.");
    }

    this.description = description;
    this.amount = amount;
    this.adjustToBusinessDay = adjustToBusinessDay;
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

  public boolean updateStatus(final RecurringOperationStatus newStatus) {

    if (newStatus == null || (this.getStatus().equals(newStatus))) {
      return false;
    }

    if (!this.getStatus().canBeUpdated(newStatus) || this.status.isTerminal()) {
      throw new IllegalStateException(
          "Recurring transaction with status [" + this.getStatus().name() +
              "] cannot have its status manually updated to [" + newStatus + "].");
    }

    switch (newStatus) {
      case ACTIVE -> this.status = RecurringOperationStatus.ACTIVE;
      case PAUSED -> this.status = RecurringOperationStatus.PAUSED;
      default -> throw new IllegalStateException("Unhandled status transition: " + newStatus);
    }
    return true;
  }

  public boolean updateFrequency(final Frequency newFrequency) {

    if (newFrequency == null || (this.getFrequency().equals(newFrequency))) {
      return false;
    }

    this.frequency = newFrequency;
    return true;
  }

  public boolean updateSchedule(final Integer dayOfTheWeek, final Integer dayOfTheMonth, final LocalDate endDate) {

    final boolean isDayOfTheWeekUnchanged = dayOfTheWeek == null || Objects.equals(this.dayOfTheWeek, dayOfTheWeek);
    final boolean isDayOfTheMonthUnchanged = dayOfTheMonth == null || Objects.equals(this.dayOfTheMonth, dayOfTheMonth);
    final boolean isEndDateUnchanged = endDate == null || Objects.equals(this.endDate, endDate);

    if (!isDayOfTheWeekUnchanged) {
      this.dayOfTheWeek = dayOfTheWeek;
    }
    if (!isDayOfTheMonthUnchanged) {
      this.dayOfTheMonth = dayOfTheMonth;
    }
    if (!isEndDateUnchanged) {
      this.endDate = endDate;
    }

    return !isDayOfTheWeekUnchanged || !isDayOfTheMonthUnchanged || !isEndDateUnchanged;
  }

  public void updateNextExecutionDate(final LocalDate nextExecutionDate) {

    if (!this.status.equals(RecurringOperationStatus.ACTIVE)) {
      return;
    }

    if (nextExecutionDate != null) {
      TemporalConfigValidator.validateExecutionDate(nextExecutionDate, this.endDate);
      this.nextExecutionDate = nextExecutionDate;
      return;
    }

    this.nextExecutionDate =
        RecurringDateCalculator.calculateNextExecution(this.frequency, this.dayOfTheWeek, this.dayOfTheMonth);
    TemporalConfigValidator.validateExecutionDate(this.nextExecutionDate, this.endDate);
  }

  public void markAsCanceled() {

    if (this.status == RecurringOperationStatus.CANCELED) {
      throw new IllegalStateException("Recurring transaction is already canceled.");
    }

    this.status = RecurringOperationStatus.CANCELED;
    this.endDate = LocalDate.now();
  }

}