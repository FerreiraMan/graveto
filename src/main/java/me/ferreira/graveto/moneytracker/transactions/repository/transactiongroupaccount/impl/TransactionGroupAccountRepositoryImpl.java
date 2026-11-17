package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroupAccount;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroupaccount.TransactionGroupAccountRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionGroupAccountRepositoryImpl implements TransactionGroupAccountRepository {

  private final TransactionGroupAccountJpaRepository repository;

  @Override
  public TransactionGroupAccount save(final TransactionGroupAccount transactionGroupAccount) {
    return repository.save(transactionGroupAccount);
  }

  @Override
  public Optional<TransactionGroupAccount> findByTransactionGroupAndAccount(final TransactionGroup transactionGroup,
                                                                            final Account account) {
    return repository.findByTransactionGroupAndAccount(transactionGroup, account);
  }

  @Override
  public List<TransactionGroup> findAllVisibleToUser(final UUID userSid) {
    return repository.findAllVisibleToUser(userSid);
  }
}
