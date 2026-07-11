package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction;

import java.time.LocalDate;
import java.util.List;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTransactionJpaRepository extends JpaRepository<RecurringTransaction, Long> {

  @Query(value = "SELECT rt FROM RecurringTransaction rt JOIN FETCH rt.account JOIN FETCH rt.category " +
      "WHERE rt.status = ?1 AND rt.nextExecutionDate <= ?2")
  List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                              final LocalDate today);

}
