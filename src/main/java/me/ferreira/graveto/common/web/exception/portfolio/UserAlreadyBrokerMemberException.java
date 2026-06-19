package me.ferreira.graveto.common.web.exception.portfolio;

import java.util.UUID;

public class UserAlreadyBrokerMemberException extends RuntimeException {
  public UserAlreadyBrokerMemberException(final UUID userSid) {
    super("The user " + userSid + " is already a member of this broker account.");

  }
}
