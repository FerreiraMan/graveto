package me.ferreira.graveto.common.web.exception.common;

public class TooManyRequestsException extends RuntimeException {
  public TooManyRequestsException() {
    super("Please wait before making another request.");
  }
}
