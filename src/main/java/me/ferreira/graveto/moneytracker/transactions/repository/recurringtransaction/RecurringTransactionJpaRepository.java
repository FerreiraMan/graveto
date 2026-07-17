package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction_;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTransactionJpaRepository extends JpaRepository<RecurringTransaction, Long>,
    JpaSpecificationExecutor<RecurringTransaction> {

  @Override
  @EntityGraph(attributePaths = {RecurringTransaction_.ACCOUNT, RecurringTransaction_.CATEGORY})
  List<RecurringTransaction> findAll(final Specification<RecurringTransaction> predicateSpec, final Sort sort);

  @Query(value = "SELECT rt FROM RecurringTransaction rt JOIN FETCH rt.account JOIN FETCH rt.category " +
      "WHERE rt.status = ?1 AND rt.nextExecutionDate <= ?2")
  List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                              final LocalDate today);

  @Query(value = "SELECT rt FROM RecurringTransaction rt JOIN FETCH rt.account WHERE rt.sid = ?1")
  Optional<RecurringTransaction> findBySid(final UUID sid);

}
