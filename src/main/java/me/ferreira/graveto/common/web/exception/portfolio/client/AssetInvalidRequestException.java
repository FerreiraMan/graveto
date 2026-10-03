package me.ferreira.graveto.common.web.exception.portfolio.client;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class AssetInvalidRequestException extends ApplicationException {
  public AssetInvalidRequestException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST,
        "An error occurred during the asset search. It may not exist, or you may not have access to it.", Level.WARN);
  }
}
