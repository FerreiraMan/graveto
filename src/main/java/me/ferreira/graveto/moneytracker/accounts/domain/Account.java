package me.ferreira.graveto.moneytracker.accounts.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.common.web.exception.moneytracker.UserAlreadyMemberException;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.hibernate.annotations.DynamicUpdate;

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

  public void addMembership(final AccountMembership membership) {

    final boolean alreadyExists = this.memberships.stream()
        .anyMatch(m -> m.getUserSid().equals(membership.getUserSid()));

    if (alreadyExists) {
      throw new UserAlreadyMemberException(membership.getUserSid());
    }

    memberships.add(membership);
    membership.setAccount(this);
  }

  public void validateUserPermission(final UUID userSid,
                                     final Predicate<MembershipRole> permissionCheck,
                                     final String actionName) {

    final boolean isAuthorized = this.memberships.stream()
        .filter(m -> userSid.equals(m.getUserSid()))
        .findFirst()
        .map(AccountMembership::getRole)
        .filter(permissionCheck)
        .isPresent();

    if (!isAuthorized) {
      throw new InsufficientPermissionsException(actionName);
    }
  }

  public void validateIsActive(final String actionName) {
    if (this.status != AccountStatus.ACTIVE) {
      throw new IllegalStateException(
          String.format("Cannot %s. The account is currently %s.", actionName, this.status.name())
      );
    }
  }

  public void updateBalance(
      final BigDecimal amount,
      final TransactionType transactionType) {

    this.balance = this.balance.add(
        amount.multiply(BigDecimal.valueOf(transactionType.getBalanceMultiplier()))
    );
  }

  public void reverseBalanceImpact(final BigDecimal amount, final TransactionType transactionType) {

    final BigDecimal reverseMultiplier = BigDecimal.valueOf(transactionType.getBalanceMultiplier()).negate();
    this.balance = this.balance.add(amount.multiply(reverseMultiplier));
  }

  public void close() {

    if (AccountStatus.CLOSED.equals(this.status)) {
      throw new IllegalStateException("This account is already closed.");
    }

    if (this.balance.compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalStateException(
          "Account balance must be exactly 0.00 before it can be closed. Current balance: " + this.balance);
    }

    this.status = AccountStatus.CLOSED;
  }

}