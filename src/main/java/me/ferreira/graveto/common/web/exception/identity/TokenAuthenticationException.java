package me.ferreira.graveto.common.web.exception.identity;

public class TokenAuthenticationException extends RuntimeException {
  public TokenAuthenticationException(final String message) {
    super(message);
  }
}
