package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;

public interface RecurringTransferRepository {

  RecurringTransfer save(RecurringTransfer recurringTransfer);

  List<RecurringTransfer> saveAll(List<RecurringTransfer> recurringTransferList);

  List<RecurringTransfer> findAll(FindAllRecurringTransfersCommand command);

  Optional<RecurringTransfer> findBySid(UUID sid);

  List<RecurringTransfer> findAllByStatusAndNextExecutionDateLessThanEqual(RecurringOperationStatus status,
                                                                              LocalDate date);

}
