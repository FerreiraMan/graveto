package me.ferreira.graveto.moneytracker.transactions.listener;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.event.AccountCreatedEvent;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountCreatedEventListener {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    @EventListener
    public Transaction onAccountCreation(final AccountCreatedEvent event) {

        final Category initialBalanceCategory = categoryService.fetchInternalCategory(SystemCategory.INITIAL_BALANCE.getSid());

        final Transaction initialTransaction = Transaction.createOpeningTransaction(
                event.account(),
                event.amount(),
                initialBalanceCategory
        );

        return transactionRepository.save(initialTransaction);
    }

}
