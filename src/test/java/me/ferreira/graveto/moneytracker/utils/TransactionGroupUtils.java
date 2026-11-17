package me.ferreira.graveto.moneytracker.utils;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionGroup;

public class TransactionGroupUtils {

  public static TransactionGroup createGroup(final String name) {
    return TransactionGroup.create(name, UUID.randomUUID());
  }

  public static Transaction createTransaction(final TransactionGroup group) {
    final Transaction transaction = new Transaction();
    transaction.setSid(UUID.randomUUID());
    transaction.setTransactionGroup(group);
    return transaction;
  }

}
