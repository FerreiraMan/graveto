package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class MaxCategoryDepthExceededException extends ApplicationException {
  public MaxCategoryDepthExceededException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT, "Category depth must be kept at 3 levels maximum.",
        Level.WARN);
  }
}
