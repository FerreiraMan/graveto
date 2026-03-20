package me.ferreira.graveto.moneytracker.accounts.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountMembership;
import me.ferreira.graveto.moneytracker.accounts.domain.MembershipRole;
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

        final AccountMembership accountMembership = AccountMembership.create(
                command.userSid(),
                MembershipRole.OWNER
        );

        account.addMembership(accountMembership);

        final Account createdAccount = accountRepository.save(account);

        transactionService.createOpeningBalance(
                createdAccount,
                createdAccount.getBalance()
        );

        return createdAccount;
    }
}
