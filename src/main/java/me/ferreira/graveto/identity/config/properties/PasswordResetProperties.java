package me.ferreira.graveto.identity.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.forgot-password")
public record PasswordResetProperties(
    Integer tokenBytes,
    Long tokenExpiration
) {
}
