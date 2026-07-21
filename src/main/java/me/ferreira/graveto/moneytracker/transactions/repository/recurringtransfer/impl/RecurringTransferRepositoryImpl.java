package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.impl;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferJpaRepository;
import me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer.RecurringTransferRepository;
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
  public List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(final RecurringOperationStatus status,
                                                                                  final LocalDate date) {
    return repository.findAllByStatusAndNextExecutionDateLessThanEqual(status, date);
  }

}
