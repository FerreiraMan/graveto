package me.ferreira.graveto.common.web.exception.portfolio;

public class InsufficientPermissionsOnBrokerException extends RuntimeException {
  public InsufficientPermissionsOnBrokerException(final String actionName) {
    super("User does not have the required role to " + actionName + " for this broker account.");
  }
}
