package me.ferreira.graveto.common.web.exception.portfolio;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(final UUID orderSid) {
    super("Broker with SID [" + orderSid + "] was not found or you do not have permission to view it.");

  }
}
