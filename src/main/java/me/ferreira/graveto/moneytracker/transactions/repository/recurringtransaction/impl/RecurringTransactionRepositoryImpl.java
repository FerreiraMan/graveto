package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.impl;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class RecurringTransactionRepositoryImpl implements RecurringTransactionRepository {

  private final RecurringTransactionJpaRepository repository;

  @Override
  public RecurringTransaction save(RecurringTransaction recurringTransaction) {
    return repository.save(recurringTransaction);
  }

  @Override
  public List<RecurringTransaction> saveAll(final List<RecurringTransaction> recurringTransactionList) {
    return repository.saveAll(recurringTransactionList);
  }

  @Override
  public List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(
      final RecurringOperationStatus status, final LocalDate date) {
    return repository.findAllByStatusAndNextExecutionDateLessThanEqual(status, date);
  }

}
