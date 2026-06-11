package me.ferreira.graveto.common.web.exception.moneytracker;

public class IllegalCategoryHierarchyException extends RuntimeException {
  public IllegalCategoryHierarchyException() {
    super("Cannot use another account's category as a parent.");
  }
}
