package me.ferreira.graveto.common.web.exception.moneytracker;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException(final String actionName) {
        super("User does not have the required role to " + actionName + " transactions for this account.");
    }
}
