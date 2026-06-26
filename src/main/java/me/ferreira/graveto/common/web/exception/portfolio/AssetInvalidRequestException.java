package me.ferreira.graveto.common.web.exception.portfolio;

public class AssetInvalidRequestException extends RuntimeException {
  public AssetInvalidRequestException(final String resource, final int statusCode) {
    super("Required resource [" + resource +
        "] is invalid or not found or requester is not authorized. HTTP status code: " + statusCode);
  }
}
