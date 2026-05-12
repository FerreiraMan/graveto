package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
  public TransactionNotFoundException(final UUID transactionSid) {
    super("Transaction with SID [" + transactionSid + "] was not found.");
  }
}
