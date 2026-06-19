package me.ferreira.graveto.common.web.exception.portfolio;

import java.util.UUID;

public class BrokerNotFoundException extends RuntimeException {

  public BrokerNotFoundException(final UUID brokerSid) {
    super("Broker with SID [" + brokerSid + "] was not found or you do not have permission to view it.");
  }

}
