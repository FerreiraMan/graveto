package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class RecurringTransactionNotFoundException extends RuntimeException {
  public RecurringTransactionNotFoundException(final UUID sid) {
    super("Recurring Transaction with SID [" + sid + "] was not found.");
  }
}
