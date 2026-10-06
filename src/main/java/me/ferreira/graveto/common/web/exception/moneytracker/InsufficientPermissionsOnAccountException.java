package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class InsufficientPermissionsOnAccountException extends ApplicationException {
  public InsufficientPermissionsOnAccountException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.FORBIDDEN, "You do not have the required role to perform this action.",
        Level.WARN);
  }
}
