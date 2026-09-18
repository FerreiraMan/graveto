package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends ApplicationException {
  public CategoryNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.NOT_FOUND, "The category account was not found.", Level.ERROR);
  }
}