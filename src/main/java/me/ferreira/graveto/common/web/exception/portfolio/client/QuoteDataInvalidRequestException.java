package me.ferreira.graveto.common.web.exception.portfolio.client;

import me.ferreira.graveto.common.web.exception.ApplicationException;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public class QuoteDataInvalidRequestException extends ApplicationException {
  public QuoteDataInvalidRequestException(final String loggableMessage) {
    super(loggableMessage, HttpStatus.BAD_REQUEST,
        "An error occurred during the asset quote date search. It may not exist, or you may not have access to it.",
        Level.WARN);
  }
}
