package me.ferreira.graveto.common.web.exception.moneytracker;

public class MaxCategoryDepthExceededException extends RuntimeException {
  public MaxCategoryDepthExceededException() {
    super("Maximum category depth exceeded. Cannot create categories deeper than level 2.");
  }
}
