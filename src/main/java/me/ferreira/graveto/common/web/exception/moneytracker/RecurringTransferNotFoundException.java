package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class RecurringTransferNotFoundException extends RuntimeException {
  public RecurringTransferNotFoundException(final UUID sid) {
    super("Recurring Transfer with SID [" + sid + "] was not found.");
  }
}
