package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class AccountWithInvalidOpeningBalanceException extends RuntimeException {
  public AccountWithInvalidOpeningBalanceException(final UUID accountSid, final String cause) {
    super("Account with SID [" + accountSid + "] with invalid amount of opening balance transactions: " + cause);
  }
}
