package me.ferreira.graveto.moneytracker.transactions.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.moneytracker.InsufficientPermissionsException;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionNotFoundException;
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
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Predicate;

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

        validateUserPermission(account, command.userSid(), MembershipRole::canCreateTransaction, "create");

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

    @Override
    @Transactional
    public Page<Transaction> findAll(final FindAllTransactionsCommand command) {

        accountService.fetchAccount(new FetchAccountCommand(command.userSid(), command.accountSid()));

        return transactionRepository.findAll(command);
    }

    @Override
    @Transactional
    public Transaction deleteTransaction(final DeleteTransactionCommand command) {

        final Transaction transaction = transactionRepository.findBySid(command.transactionSid())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionSid()));

        validateUserPermission(transaction.getAccount(), command.userSid(), MembershipRole::canDeleteTransaction, "delete");

        transaction.markAsDeleted();
        transaction.getAccount().reverseBalanceImpact(transaction.getAmount(), transaction.getType());

        return transaction;
    }

    private void validateUserPermission(final Account account, final UUID userSid, final Predicate<MembershipRole> permissionCheck,
        final String actionName) {

        final boolean isAuthorized = account.getMemberships().stream()
                .filter(m -> userSid.equals(m.getUserSid()))
                .findFirst()
                .map(AccountMembership::getRole)
                .filter(permissionCheck)
                .isPresent();

        if (!isAuthorized) {
            throw new InsufficientPermissionsException(actionName);
        }
    }

}
