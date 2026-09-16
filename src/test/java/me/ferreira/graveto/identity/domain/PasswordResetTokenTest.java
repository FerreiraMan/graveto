package me.ferreira.graveto.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PasswordResetTokenTest {

  @Test
  void shouldCreatePasswordResetToken() {
    // Arrange
    final String tokenHash = "hash";
    final long expirationTimeMs = 1000L;
    final UUID userSid = UUID.randomUUID();
    final User user = new User();
    user.setSid(userSid);
    final LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Lisbon"));

    // Act
    final PasswordResetToken passwordResetToken = PasswordResetToken.create(user, tokenHash, expirationTimeMs);

    // Assert
    assertThat(passwordResetToken.getSid()).isNotNull();
    assertThat(passwordResetToken.getUser()).isNotNull();
    assertThat(passwordResetToken.getUser().getSid()).isEqualTo(userSid);
    assertThat(passwordResetToken.getTokenHash()).isEqualTo(tokenHash);
    assertThat(passwordResetToken.getExpiresAt().truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
        now.plus(expirationTimeMs, ChronoUnit.MILLIS).truncatedTo(ChronoUnit.MINUTES));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnExpiredStatus(final boolean isExpiredToken) {
    // Arrange
    final PasswordResetToken passwordResetToken = new PasswordResetToken();
    if (isExpiredToken) {
      passwordResetToken.setExpiresAt(LocalDateTime.now().minusDays(5));
    } else {
      passwordResetToken.setExpiresAt(LocalDateTime.now().plusDays(5));
    }

    // Act
    final boolean result = passwordResetToken.isExpired();

    // Assert
    assertThat(result).isEqualTo(isExpiredToken);
  }

}
