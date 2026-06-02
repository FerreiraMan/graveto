package me.ferreira.graveto.moneytracker.accounts.service;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.command.CloseAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.command.FetchAccountCommand;
import me.ferreira.graveto.moneytracker.accounts.service.payload.AccountDetails;

public interface AccountService {

  Account createAccount(CreateAccountCommand command);

  AccountDetails fetchAccount(FetchAccountCommand command);

  List<Account> fetchAllAccounts(UUID userSid);

  Account fetchAccountEntity(UUID sid);

  Account closeAccount(CloseAccountCommand command);

}
