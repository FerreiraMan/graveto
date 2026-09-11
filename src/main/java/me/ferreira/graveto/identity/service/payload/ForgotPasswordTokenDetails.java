package me.ferreira.graveto.identity.service.payload;

public record ForgotPasswordTokenDetails(
    String token,
    Long expiresIn
) {
}
