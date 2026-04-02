package me.ferreira.graveto.common.web.exception.moneytracker;

public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
