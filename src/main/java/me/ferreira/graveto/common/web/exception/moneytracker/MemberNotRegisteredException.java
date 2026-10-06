package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class MemberNotRegisteredException extends ApplicationException {
  public MemberNotRegisteredException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT,
        "New account member needs to be registered in the platform in order to be onboarded.", Level.WARN);
  }
}
