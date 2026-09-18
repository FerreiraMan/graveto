package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class AccountWithInvalidOpeningBalanceException extends ApplicationException {
  public AccountWithInvalidOpeningBalanceException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.INTERNAL_SERVER_ERROR,
        "Something went wrong while processing the cash flow report. Please try again later or contact support.",
        Level.ERROR);
  }
}
