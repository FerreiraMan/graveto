package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.command.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TransactionService {

    Transaction createTransaction(CreateTransactionCommand command);

    Page<Transaction> findAll(FindAllTransactionsCommand command);

    Transaction deleteTransaction(DeleteTransactionCommand command);

    Transaction updateTransaction(UpdateTransactionCommand command);

    List<MonthlyAggregateProjection> generateMonthlyAggregates(GenerateMonthlyAggregateCommand command);

    List<CategoryAggregateProjection> generateCategoryAggregates(GenerateCategoryAggregateCommand command);

}
