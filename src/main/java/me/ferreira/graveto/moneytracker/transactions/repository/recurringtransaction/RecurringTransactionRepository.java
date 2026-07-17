package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;

public interface RecurringTransactionRepository {

  RecurringTransaction save(RecurringTransaction recurringTransaction);

  List<RecurringTransaction> saveAll(List<RecurringTransaction> recurringTransactionList);

  List<RecurringTransaction> findAll(FindAllRecurringTransactionsCommand command);

  Optional<RecurringTransaction> findBySid(UUID sid);

  List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(RecurringOperationStatus status,
                                                                              LocalDate date);

}
