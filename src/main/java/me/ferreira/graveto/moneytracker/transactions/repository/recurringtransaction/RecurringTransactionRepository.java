package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction;

import java.time.LocalDate;
import java.util.List;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;

public interface RecurringTransactionRepository {

  RecurringTransaction save(RecurringTransaction recurringTransaction);

  List<RecurringTransaction> saveAll(List<RecurringTransaction> recurringTransactionList);

  List<RecurringTransaction> findAllByStatusAndNextExecutionDateLessThanEqual(RecurringOperationStatus status,
                                                                              LocalDate date);

}
