package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends ApplicationException {
  public TransactionNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.NOT_FOUND,
        "This transaction is no longer available. It may have been removed, or you may not have access to it.",
        Level.WARN);
  }
}
