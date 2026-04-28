package me.ferreira.graveto.moneytracker.transactions.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.moneytracker.TransactionNotFoundException;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchCategoryCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.repository.TransactionRepository;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import me.ferreira.graveto.moneytracker.transactions.service.command.CreateTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.DeleteTransactionCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import me.ferreira.graveto.moneytracker.transactions.service.command.UpdateTransactionCommand;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        account.validateUserPermission(command.userSid(), MembershipRole::canCreateTransaction, "create");

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

        final Account account = transaction.getAccount();

        if (transaction.getCorrelationId() != null
                || transaction.getType() == TransactionType.TRANSFER_IN || transaction.getType() == TransactionType.TRANSFER_OUT) {
            throw new IllegalStateException("This transaction is part of a transfer and must be deleted via the Transfer API.");
        }

        account.validateUserPermission(command.userSid(), MembershipRole::canDeleteTransaction, "delete");

        transaction.markAsDeleted();
        account.reverseBalanceImpact(transaction.getAmount(), transaction.getType());

        return transaction;
    }

    @Override
    @Transactional
    public Transaction updateTransaction(final UpdateTransactionCommand command) {

        final Transaction transaction = transactionRepository.findBySid(command.transactionSid())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionSid()));

        validateTransactionTypeInvariants(transaction, command);

        final Account account = transaction.getAccount();

        account.validateUserPermission(command.userSid(), MembershipRole::canUpdateTransaction, "update");

        final BigDecimal effectiveAmount = command.amount() != null ? command.amount() : transaction.getAmount();
        final TransactionType effectiveType = command.transactionType() != null ? command.transactionType() : transaction.getType();
        final String effectiveDescription = command.description() != null ? command.description() : transaction.getDescription();
        final LocalDateTime effectiveOccurredAt = command.occurredAt() != null ? command.occurredAt() : transaction.getOccurredAt();
        Category effectiveCategory = transaction.getCategory();

        if (command.categorySid() != null && !command.categorySid().equals(transaction.getCategory().getSid())) {

            effectiveCategory = categoryService.fetchCategory(
                    new FetchCategoryCommand(command.userSid(), command.categorySid())
            );
        }

        final boolean requiresBalanceCalculation =
                transaction.getAmount().compareTo(effectiveAmount) != 0 ||
                transaction.getType() != effectiveType;

        if (requiresBalanceCalculation) {
            account.reverseBalanceImpact(transaction.getAmount(), transaction.getType());
        }

        transaction.updateDetails(
                effectiveAmount,
                effectiveCategory,
                effectiveDescription,
                effectiveType,
                effectiveOccurredAt
        );

        if (requiresBalanceCalculation) {
            account.updateBalance(transaction.getAmount(), transaction.getType());
        }

        return transaction;
    }

    private void validateTransactionTypeInvariants(final Transaction transaction, final UpdateTransactionCommand command) {

        if (transaction.getCorrelationId() != null) {
            throw new IllegalStateException("This transaction is part of a transfer and must be updated via the Transfer API.");
        }

        if (command.transactionType() == TransactionType.TRANSFER_IN || command.transactionType() == TransactionType.TRANSFER_OUT) {
            throw new IllegalArgumentException("Cannot change a standard transaction into a transfer. Please create a new Transfer instead.");
        }
    }

}
