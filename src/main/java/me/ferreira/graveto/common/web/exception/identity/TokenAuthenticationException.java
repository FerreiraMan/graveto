package me.ferreira.graveto.common.web.exception.identity;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class TokenAuthenticationException extends ApplicationException {
  public TokenAuthenticationException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNAUTHORIZED, "User not authorized to perform the requested action.",
        Level.ERROR);
  }
}
