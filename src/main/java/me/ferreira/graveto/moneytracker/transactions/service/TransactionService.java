package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import org.springframework.data.domain.Page;

public interface TransactionService {

    Transaction createTransaction(CreateTransactionCommand command);

    Page<Transaction> findAll(FindAllTransactionsCommand command);

    Transaction deleteTransaction(DeleteTransactionCommand command);

    Transaction updateTransaction(UpdateTransactionCommand command);

}
