package me.ferreira.graveto.moneytracker.transactions.service;

import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;

import java.math.BigDecimal;

public interface TransactionService {

    Transaction createOpeningBalance(Account account, BigDecimal openingBalance);

}
