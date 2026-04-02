package me.ferreira.graveto.moneytracker.transactions.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchCategoryCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createTransaction(final CreateTransactionCommand command) {

        final Category category = categoryService.fetchCategory(new FetchCategoryCommand(command.userSid(), command.categorySid()));

        final Account account = accountService.fetchAccount(new FetchAccountCommand(command.userSid(), command.accountSid()));

        validateUserCanCreateTransaction(account, command.userSid());

        account.updateBalance(command.amount(), command.transactionType());

        final Transaction transaction = Transaction.create(
                account,
                command.amount(),
                command.description(),
                category,
                command.transactionType(),
                command.occurredAt()
        );

        return transactionRepository.save(transaction);
    }

    private void validateUserCanCreateTransaction(final Account account, final UUID userSid) {

        final boolean canCreate = account.getMemberships().stream()
                .filter(m -> userSid.equals(m.getUserSid()))
                .findFirst()
                .map(AccountMembership::getRole)
                .map(MembershipRole::canCreateTransaction)
                .orElse(false);

        if (!canCreate) {

            throw new InsufficientPermissionsException("User does not have the required role to create transactions for this account.");
        }
    }

}
