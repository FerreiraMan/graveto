package me.ferreira.graveto.moneytracker.transactions.service;

import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CancelRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;

public interface RecurringTransactionService {

  RecurringTransaction createRecurringTransaction(CreateRecurringTransactionCommand command);

  RecurringTransaction updateRecurringTransaction(UpdateRecurringTransactionCommand command);

  List<RecurringTransaction> fetchAllRecurringTransactions(FindAllRecurringTransactionsCommand command);

  RecurringTransaction cancelRecurringTransaction(CancelRecurringTransactionCommand command);

}
