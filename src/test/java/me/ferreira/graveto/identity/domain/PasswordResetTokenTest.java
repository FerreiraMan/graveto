package me.ferreira.graveto.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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

}
