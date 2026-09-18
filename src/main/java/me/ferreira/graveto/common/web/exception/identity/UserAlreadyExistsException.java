package me.ferreira.graveto.common.web.exception.identity;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApplicationException {
  public UserAlreadyExistsException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.CONFLICT, "An account with this email already exists.", Level.WARN);
  }
}
