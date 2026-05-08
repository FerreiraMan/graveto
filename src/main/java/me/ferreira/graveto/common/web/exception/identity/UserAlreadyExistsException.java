package me.ferreira.graveto.common.web.exception.identity;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("This email is already being used.");
    }
}
