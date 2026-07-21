package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer;

import java.time.LocalDate;
import java.util.List;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;

public interface RecurringTransferRepository {

  RecurringTransfer save(RecurringTransfer recurringTransfer);

  List<RecurringTransfer> saveAll(List<RecurringTransfer> recurringTransferList);

  List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(RecurringOperationStatus status,
                                                                              LocalDate date);

}
