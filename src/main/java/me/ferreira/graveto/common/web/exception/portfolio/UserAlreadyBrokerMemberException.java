package me.ferreira.graveto.common.web.exception.portfolio;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class UserAlreadyBrokerMemberException extends ApplicationException {
  public UserAlreadyBrokerMemberException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, "This user is already a member of the broker.",
        Level.WARN);
  }
}
