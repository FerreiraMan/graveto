package me.ferreira.graveto.identity.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String signingSecret,
    Long expiration,
    List<String> issuers
) {
}
