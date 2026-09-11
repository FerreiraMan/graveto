package me.ferreira.graveto.common.mail.domain.event;

public record PasswordResetEmailEvent(
    String destinationEmail,
    String token,
    long expiresInMinutes
) {
}
