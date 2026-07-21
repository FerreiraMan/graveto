package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer;

import java.time.LocalDate;
import java.util.List;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTransferJpaRepository extends JpaRepository<RecurringTransfer, Long> {

  @Query(value = "SELECT rt FROM RecurringTransfer rt JOIN FETCH rt.sourceAccount JOIN FETCH rt.destinationAccount " +
      "WHERE rt.status = ?1 AND rt.nextExecutionDate <= ?2")
  List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                           final LocalDate today);

}
