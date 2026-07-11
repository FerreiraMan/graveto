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

  public void updateNextExecutionDate(final Long amount, final ChronoUnit temporalUnit) {
    this.lastExecutedAt = LocalDateTime.now();
    this.nextExecutionDate = this.nextExecutionDate.plus(amount, temporalUnit);

    if (this.endDate != null && this.nextExecutionDate.isAfter(endDate)) {
      this.status = RecurringOperationStatus.COMPLETED;
    }
  }

}