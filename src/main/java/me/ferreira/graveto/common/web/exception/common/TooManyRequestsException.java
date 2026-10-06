package me.ferreira.graveto.common.web.exception.common;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApplicationException {
  public TooManyRequestsException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.TOO_MANY_REQUESTS,
        "You exceeded the maximum number of requests. Please try again later.", Level.INFO);
  }
}
