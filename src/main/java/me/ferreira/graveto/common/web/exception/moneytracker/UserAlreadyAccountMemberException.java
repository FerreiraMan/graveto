package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class UserAlreadyAccountMemberException extends ApplicationException {
  public UserAlreadyAccountMemberException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, "This user is already a member of the account.",
        Level.WARN);
  }
}
