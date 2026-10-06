package me.ferreira.graveto.common.web.exception;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public abstract class ApplicationException extends RuntimeException {

  private final HttpStatus status;
  private final String safeMessage;
  private final Level logLevel;

  protected ApplicationException(final String loggableMessage, final HttpStatus status, final String safeMessage,
                                 final Level logLevel) {
    super(loggableMessage);
    this.status = status;
    this.safeMessage = safeMessage;
    this.logLevel = logLevel;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getSafeMessage() {
    return safeMessage;
  }

  public Level getLogLevel() {
    return logLevel;
  }

}
