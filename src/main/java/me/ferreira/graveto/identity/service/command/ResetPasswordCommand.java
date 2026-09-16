package me.ferreira.graveto.identity.service.command;

public record ResetPasswordCommand(
    String token,
    String newPassword
) {
}
