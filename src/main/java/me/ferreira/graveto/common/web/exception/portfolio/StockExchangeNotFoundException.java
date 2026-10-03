package me.ferreira.graveto.common.web.exception.portfolio;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class StockExchangeNotFoundException extends ApplicationException {
  public StockExchangeNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST, "The exchange in this symbol is not supported.", Level.WARN);
  }
}
