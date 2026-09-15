package me.ferreira.graveto.common.web.exception.moneytracker;

public class IllegalCategoryHierarchyException extends RuntimeException {
  public IllegalCategoryHierarchyException(final String message) {
    super(message);
  }
}
