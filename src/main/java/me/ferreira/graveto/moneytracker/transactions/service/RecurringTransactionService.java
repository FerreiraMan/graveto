package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.CreateRecurringTransactionCommand;

public interface RecurringTransactionService {

  RecurringTransaction createRecurringTransaction(CreateRecurringTransactionCommand command);

}
