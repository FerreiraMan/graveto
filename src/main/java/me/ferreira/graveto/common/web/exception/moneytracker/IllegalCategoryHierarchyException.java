package me.ferreira.graveto.common.web.exception.moneytracker;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class IllegalCategoryHierarchyException extends ApplicationException {
  public IllegalCategoryHierarchyException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.UNPROCESSABLE_CONTENT,
        "Category transaction type must match the parent category's transaction type.", Level.WARN);
  }
}
