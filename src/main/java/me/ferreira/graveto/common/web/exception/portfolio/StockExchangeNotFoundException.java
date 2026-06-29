package me.ferreira.graveto.common.web.exception.portfolio;

public class StockExchangeNotFoundException extends RuntimeException {
  public StockExchangeNotFoundException(final String suffix) {
    super("Stock exchange with suffix [" + suffix + "] was not found.");
  }
}
