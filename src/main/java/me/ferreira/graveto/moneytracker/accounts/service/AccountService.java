package me.ferreira.graveto.moneytracker.accounts.service;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.service.command.CreateAccountCommand;

public interface AccountService {

    Account createAccount(CreateAccountCommand command);
}
