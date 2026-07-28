package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer_;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransfersSpecs;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class RecurringTransferRepositoryImpl implements RecurringTransferRepository {

  private final RecurringTransferJpaRepository repository;

  @Override
  public RecurringTransfer save(final RecurringTransfer recurringTransfer) {
    return repository.save(recurringTransfer);
  }

  @Override
  public List<RecurringTransfer> saveAll(final List<RecurringTransfer> recurringTransferList) {
    return repository.saveAll(recurringTransferList);
  }

  @Override
  public List<RecurringTransfer> findAll(final FindAllRecurringTransfersCommand command) {

    final PredicateSpecification<RecurringTransfer> predicateSpec =
        RecurringTransfersSpecs.buildFromCommand(command);
    final Specification<RecurringTransfer> classicSpec = Specification.where(predicateSpec);
    final Sort sortByEarliestExecutionDate = Sort.by(RecurringTransfer_.NEXT_EXECUTION_DATE);

    return repository.findAll(classicSpec, sortByEarliestExecutionDate);
  }

  @Override
  public Optional<RecurringTransfer> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

  @Override
  public List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                                  final LocalDate date) {
    return repository.findAllByStatusAndNextExecutionDateLessThanEqual(status, date);
  }

}
