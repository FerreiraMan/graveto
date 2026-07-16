package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.UpdateRecurringTransactionCommand;

public interface RecurringTransactionService {

  RecurringTransaction createRecurringTransaction(CreateRecurringTransactionCommand command);

  RecurringTransaction updateRecurringTransaction(UpdateRecurringTransactionCommand command);

}
