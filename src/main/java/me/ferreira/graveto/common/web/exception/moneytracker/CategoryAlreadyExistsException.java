package me.ferreira.graveto.common.web.exception.moneytracker;

public class CategoryAlreadyExistsException extends RuntimeException {
  public CategoryAlreadyExistsException(final String name) {
    super("Category with name [" + name + "] already exists.");
  }
}
