package me.ferreira.graveto.common.web.exception.portfolio;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class InvalidExchangeException extends ApplicationException {
  public InvalidExchangeException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST,
        "An error occurred during the asset creation. The requested exchange may be invalid. Please contact support.",
        Level.WARN);
  }
}
