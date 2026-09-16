package me.ferreira.graveto.common.web.exception.identity;

public class InvalidResetPasswordTokenException extends RuntimeException {
  public InvalidResetPasswordTokenException(final String message) {
    super(message);
  }
}
