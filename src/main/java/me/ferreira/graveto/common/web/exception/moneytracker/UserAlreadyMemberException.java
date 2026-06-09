package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class UserAlreadyMemberException extends RuntimeException {
  public UserAlreadyMemberException(final UUID userSid) {
    super("The user " + userSid + " is already a member of this account.");
  }
}
