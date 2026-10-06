package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class TransferWithInvalidTransactionsException extends ApplicationException {
  public TransferWithInvalidTransactionsException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.INTERNAL_SERVER_ERROR,
        "Something went wrong while processing the request on the specified transfer. Please contact support.",
        Level.ERROR);
  }
}
