package me.ferreira.graveto.common.web.exception.moneytracker;

import java.util.UUID;

public class UserAlreadyAccountMemberException extends RuntimeException {
  public UserAlreadyAccountMemberException(final UUID userSid) {
    super("The user " + userSid + " is already a member of this account.");
  }
}
