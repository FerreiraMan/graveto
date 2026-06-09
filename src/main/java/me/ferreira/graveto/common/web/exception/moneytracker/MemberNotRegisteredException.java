package me.ferreira.graveto.common.web.exception.moneytracker;

public class MemberNotRegisteredException extends RuntimeException {
  public MemberNotRegisteredException(final String email) {
    super("User " + email + " needs to be registered in the platform in order enable account memberships.");
  }
}
