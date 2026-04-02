package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;

public interface TransactionService {

    Transaction createTransaction(CreateTransactionCommand command);

}
