package me.ferreira.graveto.common.web.exception.portfolio;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class AssetNotFoundException extends ApplicationException {
  public AssetNotFoundException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.NOT_FOUND,
        "This asset is currently not available. Please make sure you add it to the list before associating orders.",
        Level.WARN);
  }
}
