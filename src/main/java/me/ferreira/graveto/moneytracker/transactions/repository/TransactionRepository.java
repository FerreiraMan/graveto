package me.ferreira.graveto.moneytracker.transactions.repository;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Page<Transaction> findAllByAccountId(Long accountId, Pageable pageable);

    List<Transaction> findAllByCorrelationId(UUID correlationId);

    BigDecimal calculateBalance(Long accountId, TransactionStatus transactionStatus);

}
