package me.ferreira.graveto.moneytracker.transactions.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransactionJpaRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

  @EntityGraph(attributePaths = {Transaction_.ACCOUNT})
  List<Transaction> findAllByCorrelationId(UUID correlationId);

  Page<Transaction> findAllByAccountId(final Long accountId, final Pageable pageable);

  @Query(value = "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = ?1 AND t.status = ?2")
  BigDecimal calculateBalance(final Long accountId, final TransactionStatus transactionStatus);

  @Override
  @NonNull
  @EntityGraph(attributePaths = {Transaction_.CATEGORY})
  Page<Transaction> findAll(@Nullable final Specification<Transaction> spec, @NonNull final Pageable pageable);

  Optional<Transaction> findBySid(final UUID sid);

  @Query(value =
      "SELECT EXTRACT(MONTH FROM t.occurredAt) AS month, t.type AS type, SUM(t.amount) AS totalAmount " +
          "FROM Transaction t " +
          "WHERE EXTRACT(YEAR FROM t.occurredAt) = ?1 " +
          "AND t.account.sid = ?2 " +
          "AND t.status = ?3 " +
          "GROUP BY EXTRACT(MONTH FROM t.occurredAt), t.type")
  List<MonthlyAggregateProjection> calculateMonthlyAggregates(final int year, final UUID accountSid,
                                                              final TransactionStatus status);

  @Query(value = "SELECT EXTRACT(MONTH FROM t.occurredAt) AS month, " +
      "c.sid AS categorySid, " +
      "SUM(t.amount) AS totalAmount " +
      "FROM Transaction t " +
      "JOIN t.category c " +
      "WHERE EXTRACT(YEAR FROM t.occurredAt) = :year " +
      "AND t.account.sid = :accountSid " +
      "AND t.status = :status " +
      "AND t.type = :type " +
      "AND c.isInternal = false " +
      "GROUP BY EXTRACT(MONTH FROM t.occurredAt), c.sid")
  List<CategoryAggregateProjection> calculateCategoryAggregates(final int year, final UUID accountSid,
                                                                final TransactionStatus status,
                                                                final TransactionType type);

}
