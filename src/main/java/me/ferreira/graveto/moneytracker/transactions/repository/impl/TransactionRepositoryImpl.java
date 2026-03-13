package me.ferreira.graveto.moneytracker.transactions.repository.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final TransactionJpaRepository repository;

    @Override
    public Transaction save(final Transaction transaction) {
        return repository.save(transaction);
    }

    @Override
    public Page<Transaction> findAllByAccountId(final Long accountId, final Pageable pageable) {
        return repository.findAllByAccountId(accountId, pageable);
    }

    @Override
    public List<Transaction> findAllByCorrelationId(final UUID correlationId) {
        return repository.findAllByCorrelationId(correlationId);
    }

    @Override
    public BigDecimal calculateBalance(final Long accountId, final TransactionStatus transactionStatus) {
        return repository.calculateBalance(accountId, transactionStatus);
    }

}
