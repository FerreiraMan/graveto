package me.ferreira.graveto.moneytracker.transactions.repository.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionsSpecs;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

  private final TransactionJpaRepository repository;

  @Override
  public Transaction save(final Transaction transaction) {
    return repository.save(transaction);
  }

  @Override
  public List<Transaction> saveAll(List<Transaction> transactions) {
    return repository.saveAll(transactions);
  }

  @Override
  public Page<Transaction> findAllByAccountId(final Long accountId, final Pageable pageable) {
    return repository.findAllByAccountId(accountId, pageable);
  }

  @Override
  public Optional<Transaction> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

  @Override
  public List<Transaction> findAllByCorrelationId(final UUID correlationId) {
    return repository.findAllByCorrelationId(correlationId);
  }

  @Override
  public BigDecimal calculateBalance(final Long accountId, final TransactionStatus transactionStatus) {
    return repository.calculateBalance(accountId, transactionStatus);
  }

  @Override
  public Page<Transaction> findAll(final FindAllTransactionsCommand command) {

    final PredicateSpecification<Transaction> predicateSpec = TransactionsSpecs.buildFromCommand(command);
    final Specification<Transaction> classicSpec = Specification.where(predicateSpec);

    return repository.findAll(classicSpec, command.pageable());
  }

  @Override
  public List<MonthlyAggregateProjection> calculateMonthlyAggregates(final int year,
                                                                     final UUID accountSid,
                                                                     final TransactionStatus status) {
    return repository.calculateMonthlyAggregates(year, accountSid, status);
  }

  @Override
  public List<CategoryAggregateProjection> calculateCategoryAggregates(final int year,
                                                                       final UUID accountSid,
                                                                       final TransactionStatus status,
                                                                       final TransactionType type) {
    return repository.calculateCategoryAggregates(year, accountSid, status, type);
  }

}
