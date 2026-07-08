package me.ferreira.graveto.common.web.exception.portfolio;

import java.util.UUID;

public class PositionNotFoundException extends RuntimeException {
  public PositionNotFoundException(final UUID positionSid) {
    super("Position with SID [" + positionSid + "] was not found or you do not have permission to view it.");
  }
}
