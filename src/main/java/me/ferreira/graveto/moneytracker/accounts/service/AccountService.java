package me.ferreira.graveto.moneytracker.accounts.service;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    Account createAccount(CreateAccountCommand command);

    Account fetchAccount(FetchAccountCommand command);

    List<Account> fetchAllAccounts(UUID userSid);

}
