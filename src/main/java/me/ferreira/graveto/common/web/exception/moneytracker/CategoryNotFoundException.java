package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {
  public CategoryNotFoundException(final UUID categorySid) {
    super("Category with SID [" + categorySid + "] was not found or does not belong to the user.");
  }
}
