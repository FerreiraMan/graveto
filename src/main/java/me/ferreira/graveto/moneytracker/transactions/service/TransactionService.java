package me.ferreira.graveto.moneytracker.transactions.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.CategoryAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.GenerateCategoryAggregateCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.GenerateMonthlyAggregateCommand;
import me.ferreira.graveto.moneytracker.transactions.service.transaction.command.UpdateTransactionCommand;
import org.springframework.data.domain.Page;

public interface TransactionService {

  Transaction createTransaction(CreateTransactionCommand command);

  Page<Transaction> findAll(FindAllTransactionsCommand command);

  List<Transaction> findAllBySidFromAccountAndStatus(Set<UUID> sids, UUID accountSid,
                                                     TransactionStatus transactionStatus);

  Long countByTransactionGroup(TransactionGroup transactionGroup);

  Transaction deleteTransaction(DeleteTransactionCommand command);

  Transaction updateTransaction(UpdateTransactionCommand command);

  List<MonthlyAggregateProjection> generateMonthlyAggregates(GenerateMonthlyAggregateCommand command);

  List<CategoryAggregateProjection> generateCategoryAggregates(GenerateCategoryAggregateCommand command);

}
