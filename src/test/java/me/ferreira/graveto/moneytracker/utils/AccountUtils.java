package me.ferreira.graveto.moneytracker.utils;

import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.accounts.domain.AccountStatus;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountUtils {

    public static Account createAccount(final BigDecimal balance) {

        final Account account = new Account();
        account.setSid(UUID.randomUUID());
        account.setInstitution("Santander");
        account.setBaseCurrency(Currency.EUR);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(balance);
        return account;
    }

}
