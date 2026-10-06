package me.ferreira.graveto.common.web.exception.common;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApplicationException {
  public InvalidRequestException(final String loggableMessage, final String safeMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST, safeMessage, Level.WARN);
  }

  public InvalidRequestException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST, loggableMessage, Level.WARN);
  }
}
