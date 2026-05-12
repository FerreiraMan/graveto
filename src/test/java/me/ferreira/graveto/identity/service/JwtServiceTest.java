package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.domain.Role;
import me.ferreira.graveto.identity.service.payload.JwtPayload;
import org.junit.jupiter.api.Test;

public class JwtServiceTest {

  private final JwtService jwtService = new JwtService("secret-key", 3600000, List.of("graveto-api"));

  @Test
  void shouldCreateAndSuccessfullyVerifyToken() {
    // Arrange
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);

    // Act
    final String token = jwtService.createJwtToken(user);
    final JwtPayload payload = jwtService.verifyJwtToken(token);

    // Assert
    assertThat(payload.sid()).isEqualTo(user.sid());
    assertThat(payload.role()).isEqualTo(Role.USER.name());
  }

  @Test
  void shouldThrowIfTokenWasTampered() {
    // Arrange
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);
    final String token = jwtService.createJwtToken(user);
    final String tamperedToken = token + "abc";

    // Act & Assert
    assertThatThrownBy(() -> {
      jwtService.verifyJwtToken(tamperedToken);
    }).isInstanceOf(TokenAuthenticationException.class)
        .hasMessage("Invalid JWT token.");
  }

  @Test
  void shouldThrowIfTokenIsFromDifferentIssuer() {
    // Arrange
    final JwtService diffIssuerService = new JwtService("secret-key", 3600000, List.of("issuer x"));
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);
    final String token = jwtService.createJwtToken(user);

    // Act & Assert
    assertThatThrownBy(() -> {
      diffIssuerService.verifyJwtToken(token);
    }).isInstanceOf(TokenAuthenticationException.class)
        .hasMessage("Invalid JWT token.");
  }

}
