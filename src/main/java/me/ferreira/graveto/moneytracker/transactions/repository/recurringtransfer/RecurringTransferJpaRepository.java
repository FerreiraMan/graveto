package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer_;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTransferJpaRepository extends JpaRepository<RecurringTransfer, Long>,
    JpaSpecificationExecutor<RecurringTransfer> {

  @Override
  @EntityGraph(attributePaths = {RecurringTransfer_.SOURCE_ACCOUNT, RecurringTransfer_.DESTINATION_ACCOUNT})
  List<RecurringTransfer> findAll(final Specification<RecurringTransfer> predicateSpec, final Sort sort);

  @Query(value = "SELECT rt FROM RecurringTransfer rt JOIN FETCH rt.sourceAccount JOIN FETCH rt.destinationAccount " +
      "WHERE rt.status = ?1 AND rt.nextExecutionDate <= ?2")
  List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                           final LocalDate today);

  @Query(value = "SELECT rt FROM RecurringTransfer rt JOIN FETCH rt.sourceAccount JOIN FETCH rt.destinationAccount " +
      "WHERE rt.sid = ?1")
  Optional<RecurringTransfer> findBySid(final UUID sid);

}
