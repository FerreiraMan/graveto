package me.ferreira.graveto.moneytracker.transactions.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.jpa.BaseEntity;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaction_group_accounts")
public class TransactionGroupAccount extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_group_accounts_id_seq")
  @SequenceGenerator(
      name = "transaction_group_accounts_id_seq",
      sequenceName = "transaction_group_accounts_id_seq",
      allocationSize = 1)
  private Long id;

  @JoinColumn(name = "group_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private TransactionGroup transactionGroup;

  @JoinColumn(name = "account_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Account account;

  public static TransactionGroupAccount create(final TransactionGroup transactionGroup, final Account account) {
    final TransactionGroupAccount transactionGroupAccount = new TransactionGroupAccount();
    transactionGroupAccount.setTransactionGroup(transactionGroup);
    transactionGroupAccount.setAccount(account);
    return transactionGroupAccount;
  }

}
