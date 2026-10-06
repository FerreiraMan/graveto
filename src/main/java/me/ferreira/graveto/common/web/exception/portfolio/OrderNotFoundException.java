package me.ferreira.graveto.common.web.exception.portfolio;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApplicationException {
  public OrderNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.NOT_FOUND,
        "This order is no longer available. It may have been removed, or you may not have access to it.", Level.WARN);
  }
}
