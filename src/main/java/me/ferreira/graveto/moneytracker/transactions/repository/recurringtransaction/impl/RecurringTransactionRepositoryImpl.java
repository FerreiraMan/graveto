package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction_;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction.RecurringTransactionsSpecs;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
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
  public List<RecurringTransaction> findAll(FindAllRecurringTransactionsCommand command) {

    final PredicateSpecification<RecurringTransaction> predicateSpec =
        RecurringTransactionsSpecs.buildFromCommand(command);
    final Specification<RecurringTransaction> classicSpec = Specification.where(predicateSpec);
    final Sort sortByEarliestExecutionDate = Sort.by(RecurringTransaction_.NEXT_EXECUTION_DATE);

    return repository.findAll(classicSpec, sortByEarliestExecutionDate);
  }

  @Override
  public Optional<RecurringTransaction> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

  @Override
  public List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(
      final RecurringOperationStatus status, final LocalDate date) {
    return repository.findAllByStatusAndNextExecutionDateLessThanEqual(status, date);
  }

}
