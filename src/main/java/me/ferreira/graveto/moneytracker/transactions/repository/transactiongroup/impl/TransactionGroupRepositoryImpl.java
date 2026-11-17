package me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.transactiongroup.TransactionGroupRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionGroupRepositoryImpl implements TransactionGroupRepository {

  private final TransactionGroupJpaRepository repository;

  @Override
  public TransactionGroup save(final TransactionGroup transactionGroup) {
    return repository.save(transactionGroup);
  }

  @Override
  public List<TransactionGroup> findOldestByName(final String name,
                                                 final List<TransactionGroup> visibleGroups) {
    return repository.findOldestByName(name, visibleGroups);
  }

}
