package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApplicationException {
  public AccountNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.NOT_FOUND,
        "The specified account is no longer available. It may have been removed, or you may not have access to it.",
        Level.WARN);
  }

}
