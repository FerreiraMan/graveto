package me.ferreira.graveto.common.web.exception.identity;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class InvalidResetPasswordTokenException extends ApplicationException {
  public InvalidResetPasswordTokenException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNAUTHORIZED, "Unable to process password reset request.", Level.WARN);
  }
}
