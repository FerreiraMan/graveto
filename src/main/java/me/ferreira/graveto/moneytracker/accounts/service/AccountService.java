package me.ferreira.graveto.moneytracker.accounts.service;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;

public interface AccountService {

  Account createAccount(CreateAccountCommand command);

  Account fetchAccount(FetchAccountCommand command);

  List<Account> fetchAllAccounts(UUID userSid);

}
