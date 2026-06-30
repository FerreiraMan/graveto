package me.ferreira.graveto.common.web.exception.portfolio.client;

public class QuoteDataInvalidRequestException extends RuntimeException {
  public QuoteDataInvalidRequestException(final String resource, final int statusCode) {
    super("Required resource [" + resource +
        "] is invalid or not found or requester is not authorized. HTTP status code: " + statusCode);
  }
}
