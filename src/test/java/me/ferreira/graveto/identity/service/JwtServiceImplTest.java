package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.web.exception.identity.TokenAuthenticationException;
import me.ferreira.graveto.identity.config.properties.JwtProperties;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.domain.Role;
import me.ferreira.graveto.identity.service.impl.JwtServiceImpl;
import me.ferreira.graveto.identity.service.payload.JwtPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JwtServiceImplTest {

  private final JwtProperties jwtProperties = new JwtProperties("secret-key", 3600000L, List.of("graveto-api"));

  @InjectMocks
  private JwtServiceImpl service = new JwtServiceImpl(jwtProperties);

  @Test
  void shouldCreateAndSuccessfullyVerifyToken() {
    // Arrange
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);

    // Act
    final String token = service.createJwtToken(user);
    final JwtPayload payload = service.verifyJwtToken(token);

    // Assert
    assertThat(payload.sid()).isEqualTo(user.sid());
    assertThat(payload.role()).isEqualTo(Role.USER.name());
  }

  @Test
  void shouldThrowIfTokenWasTampered() {
    // Arrange
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);
    final String token = service.createJwtToken(user);
    final String tamperedToken = token + "abc";

    // Act & Assert
    assertThatThrownBy(() -> {
      service.verifyJwtToken(tamperedToken);
    }).isInstanceOf(TokenAuthenticationException.class)
        .hasMessage("Invalid JWT token.");
  }

  @Test
  void shouldThrowIfTokenIsFromDifferentIssuer() {
    // Arrange
    final JwtProperties diffIssuerProps = new JwtProperties("secret-key", 3600000L, List.of("issuer-x"));
    final JwtServiceImpl diffIssuerService = new JwtServiceImpl(diffIssuerProps);
    final AuthUser user = new AuthUser(UUID.randomUUID(), "test@graveto.com", "hash", Role.USER);
    final String token = service.createJwtToken(user);

    // Act & Assert
    assertThatThrownBy(() -> {
      diffIssuerService.verifyJwtToken(token);
    }).isInstanceOf(TokenAuthenticationException.class)
        .hasMessage("Invalid JWT token.");
  }

}
