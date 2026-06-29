package me.ferreira.graveto.common.web.exception.portfolio;

public class InvalidExchangeException extends RuntimeException {
  public InvalidExchangeException() {
    super("The requested exchange is invalid. Please contact support.");
  }
}
