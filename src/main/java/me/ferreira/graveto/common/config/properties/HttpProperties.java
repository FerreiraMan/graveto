package me.ferreira.graveto.common.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "http")
public record HttpProperties(
    CorsProperties cors,
    ClientProperties client
) {
  public record CorsProperties(
      List<String> allowedOrigins
  ) {
  }

  public record ClientProperties(
      Integer connectTimeout,
      Integer readTimeout,
      Integer maxRequests,
      Integer requestCooldown
  ) {
  }
}