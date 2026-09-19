package me.ferreira.graveto.common.web.exception.common;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class ExternalApiUnavailableException extends ApplicationException {
  public ExternalApiUnavailableException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_GATEWAY, "An error occurred during the request to the external server.",
        Level.ERROR);
  }
}
