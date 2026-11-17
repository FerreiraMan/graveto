package me.ferreira.graveto.common.web.exception.common;

import java.util.Map;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ApplicationException {
  public BusinessRuleViolationException(final String loggableMessage, final String safeMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, safeMessage, Level.WARN);
  }

  public BusinessRuleViolationException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, loggableMessage, Level.WARN);
  }

  public BusinessRuleViolationException(final String loggableMessage, final String safeMessage,
                                        final Map<String, Object> extraProperties) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, safeMessage, Level.WARN, extraProperties);
  }

}
