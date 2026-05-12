package me.ferreira.graveto.moneytracker.transactions.service;

import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateCategoryAggregateCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.GenerateMonthlyAggregateCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import org.springframework.data.domain.Page;

public interface TransactionService {

  Transaction createTransaction(CreateTransactionCommand command);

  Page<Transaction> findAll(FindAllTransactionsCommand command);

  Transaction deleteTransaction(DeleteTransactionCommand command);

  Transaction updateTransaction(UpdateTransactionCommand command);

  List<MonthlyAggregateProjection> generateMonthlyAggregates(GenerateMonthlyAggregateCommand command);

  List<CategoryAggregateProjection> generateCategoryAggregates(GenerateCategoryAggregateCommand command);

}
