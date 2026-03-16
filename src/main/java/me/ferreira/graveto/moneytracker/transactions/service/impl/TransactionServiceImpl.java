package me.ferreira.graveto.moneytracker.transactions.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createOpeningBalance(final Account account, final BigDecimal openingBalance) {

        final Category initialBalanceCategory = categoryService.getInitialBalanceCategory();

        final Transaction initialTransaction = Transaction.createOpeningTransaction(
                account,
                openingBalance,
                initialBalanceCategory
        );

        return transactionRepository.save(initialTransaction);
    }

}
