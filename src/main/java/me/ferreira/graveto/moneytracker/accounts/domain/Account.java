package me.ferreira.graveto.moneytracker.accounts.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    private static final BigDecimal DEFAULT_ACCOUNT_OPENING_BALANCE = BigDecimal.ZERO;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_id_seq")
    @SequenceGenerator(name = "accounts_id_seq", sequenceName = "accounts_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID sid;

    @Column(nullable = false)
    private BigDecimal balance = DEFAULT_ACCOUNT_OPENING_BALANCE;

    @Column(nullable = false, name = "base_currency")
    @Enumerated(EnumType.STRING)
    private Currency baseCurrency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column
    private String institution;

    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<AccountMembership> memberships = new ArrayList<>();

    public void addMembership(final AccountMembership membership) {
        memberships.add(membership);
        membership.setAccount(this);
    }

    public static Account create(
            final BigDecimal initialBalance,
            final Currency baseCurrency,
            final String institution) {

        final Account acc = new Account();

        acc.setSid(UUID.randomUUID());

        acc.setBalance(initialBalance);
        acc.setBaseCurrency(baseCurrency);
        acc.setInstitution(institution);

        acc.setStatus(AccountStatus.ACTIVE);

        return acc;
    }

    public void updateBalance(
            final BigDecimal amount,
            final TransactionType transactionType) {

        this.balance = this.balance.add(
                amount.multiply(BigDecimal.valueOf(transactionType.getBalanceMultiplier()))
        );
    }

}