package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class CategoryAlreadyExistsException extends ApplicationException {
  public CategoryAlreadyExistsException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.CONFLICT, "A category with this name already exists.", Level.WARN);
  }
}
