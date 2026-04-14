package me.ferreira.graveto.moneytracker.transactions.repository;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByCorrelationId(UUID correlationId);

    Page<Transaction> findAllByAccountId(final Long accountId, final Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = ?1 AND t.status = ?2")
    BigDecimal calculateBalance(final Long accountId, final TransactionStatus transactionStatus);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {Transaction_.CATEGORY})
    Page<Transaction> findAll(@Nullable final Specification<Transaction> spec, @NonNull final Pageable pageable);

    Optional<Transaction> findBySid(final UUID sid);
}
