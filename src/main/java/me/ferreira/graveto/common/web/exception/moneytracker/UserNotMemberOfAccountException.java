package me.ferreira.graveto.common.web.exception.moneytracker;

public class UserNotMemberOfAccountException extends RuntimeException {
  public UserNotMemberOfAccountException() {
    super("The user is not a member of this account.");
  }
}
