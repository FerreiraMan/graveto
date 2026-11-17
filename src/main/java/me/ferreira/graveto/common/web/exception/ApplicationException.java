package me.ferreira.graveto.common.web.exception;

import java.util.Map;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public abstract class ApplicationException extends RuntimeException {

  private final HttpStatus status;
  private final String safeMessage;
  private final Level logLevel;
  private final Map<String, Object> extraProperties;

  protected ApplicationException(final String loggableMessage, final HttpStatus status, final String safeMessage,
                                 final Level logLevel) {
    this(loggableMessage, status, safeMessage, logLevel, Map.of());
  }

  protected ApplicationException(final String loggableMessage, final HttpStatus status, final String safeMessage,
                                 final Level logLevel, final Map<String, Object> extraProperties) {
    super(loggableMessage);
    this.status = status;
    this.safeMessage = safeMessage;
    this.logLevel = logLevel;
    this.extraProperties = extraProperties;
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

  public Map<String, Object> getExtraProperties() {
    return extraProperties;
  }

}
