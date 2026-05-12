package me.ferreira.graveto.moneytracker.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.moneytracker.accounts.domain.Account;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;

public class TransactionTestFactory {

  public static Transaction createTransaction(final Account account, final Category category,
                                              final TransactionType type, final BigDecimal amount,
                                              final TransactionStatus status, final LocalDate date) {

    final Transaction tx = new Transaction();

    tx.setSid(UUID.randomUUID());
    tx.setCurrency(Currency.EUR);
    tx.setAccount(account);
    tx.setCategory(category);
    tx.setType(type);
    tx.setAmount(amount);
    tx.setStatus(status);
    tx.setOccurredAt(date.atTime(12, 0));

    return tx;
  }

}
