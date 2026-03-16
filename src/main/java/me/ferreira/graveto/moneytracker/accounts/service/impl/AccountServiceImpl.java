package me.ferreira.graveto.moneytracker.accounts.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.repository.AccountRepository;
import me.ferreira.graveto.moneytracker.accounts.service.AccountService;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account createAccount(final CreateAccountCommand command) {

        final Account account = Account.create(
                command.initialBalance(),
                command.baseCurrency(),
                command.institution()
        );

        final Account createdAccount = accountRepository.save(account);

        transactionService.createOpeningBalance(
                createdAccount,
                createdAccount.getBalance()
        );

        //TODO add membership of user that is to be intercepted by the security filter

        return createdAccount;
    }
}
