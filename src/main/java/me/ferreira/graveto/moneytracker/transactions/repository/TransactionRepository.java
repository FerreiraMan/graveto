package me.ferreira.graveto.moneytracker.transactions.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository {

  Transaction save(Transaction transaction);

  List<Transaction> saveAll(List<Transaction> transactions);

  Page<Transaction> findAllByAccountId(Long accountId, Pageable pageable);

  Optional<Transaction> findBySid(UUID sid);

  List<Transaction> findAllByCorrelationId(UUID correlationId);

  BigDecimal calculateBalance(Long accountId, TransactionStatus transactionStatus);

  Page<Transaction> findAll(FindAllTransactionsCommand command);

  List<MonthlyAggregateProjection> calculateMonthlyAggregates(int year, UUID accountSid, TransactionStatus status);

  List<CategoryAggregateProjection> calculateCategoryAggregates(int year, UUID accountSid, TransactionStatus status,
                                                                TransactionType type);

}
