package me.ferreira.graveto.moneytracker.transactions.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transactions_id_seq")
    @SequenceGenerator(name = "transactions_id_seq", sequenceName = "transactions_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID sid;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @JoinColumn(name = "account_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Account account;

    @JoinColumn(name = "category_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public static Transaction createOpeningTransaction(
            final Account account,
            final BigDecimal amount,
            final Category category) {

        final Transaction tx = new Transaction();

        tx.setSid(UUID.randomUUID());
        tx.setOccurredAt(LocalDateTime.now());

        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setCurrency(account.getBaseCurrency());
        tx.setCategory(category);

        tx.setType(TransactionType.OPENING_BALANCE);
        tx.setStatus(TransactionStatus.ACTIVE);

        return tx;
    }

    public static Transaction create(
            final Account account,
            final BigDecimal amount,
            final String description,
            final Category category,
            final TransactionType transactionType,
            final LocalDateTime occurredAt) {

        final Transaction tx = new Transaction();

        tx.setSid(UUID.randomUUID());
        tx.setOccurredAt(occurredAt);

        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setCurrency(account.getBaseCurrency());
        tx.setCategory(category);

        tx.setType(transactionType);
        tx.setStatus(TransactionStatus.ACTIVE);

        return tx;
    }

    public static Transaction createTransferTransaction(
            final Account account,
            final BigDecimal amount,
            final String description,
            final UUID correlationId,
            final Category category,
            final TransactionType transactionType,
            final LocalDateTime occurredAt) {

        final Transaction tx = new Transaction();

        tx.setSid(UUID.randomUUID());
        tx.setOccurredAt(occurredAt);

        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setCorrelationId(correlationId);
        tx.setCurrency(account.getBaseCurrency());
        tx.setCategory(category);

        tx.setType(transactionType);
        tx.setStatus(TransactionStatus.ACTIVE);

        return tx;
    }

    public void markAsDeleted() {

        if (this.status == TransactionStatus.DELETED) {
            throw new IllegalStateException("Transaction is already deleted.");
        }

        this.status = TransactionStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateDetails(final BigDecimal amount,
                              final Category category,
                              final String description,
                              final TransactionType transactionType,
                              final LocalDateTime occurredAt) {

        if (this.status == TransactionStatus.DELETED) {
            throw new IllegalStateException("Cannot update a deleted transaction.");
        }

        this.amount = amount;
        this.category = category;
        this.description = description;
        this.type = transactionType;
        this.occurredAt = occurredAt;
    }

}
