package me.ferreira.graveto.common.web.exception.common;

public class ExternalApiUnavailableException extends RuntimeException {
  public ExternalApiUnavailableException(final int statusCode) {
    super("Upstream server is currently unavailable. HTTP status code: " + statusCode);
  }
}
