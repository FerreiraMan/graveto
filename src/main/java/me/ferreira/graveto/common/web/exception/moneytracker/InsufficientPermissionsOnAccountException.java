package me.ferreira.graveto.common.web.exception.moneytracker;

public class InsufficientPermissionsOnAccountException extends RuntimeException {
  public InsufficientPermissionsOnAccountException(final String actionName) {
    super("User does not have the required role to " + actionName + " for this account.");
  }
}
