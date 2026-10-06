package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class UserNotMemberOfAccountException extends ApplicationException {
  public UserNotMemberOfAccountException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.FORBIDDEN, "You do not have access to this account.", Level.WARN);
  }
}
