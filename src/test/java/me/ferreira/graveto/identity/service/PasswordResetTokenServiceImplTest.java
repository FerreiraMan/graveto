package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import me.ferreira.graveto.identity.config.properties.PasswordResetProperties;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.impl.PasswordResetTokenServiceImpl;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PasswordResetTokenServiceImplTest {

  private static final long THIRTY_MINUTES_MILLIS = 1_800_000L;

  @InjectMocks
  private PasswordResetTokenServiceImpl service;
  @Mock
  private PasswordResetTokenRepository resetTokenRepository;
  @Mock
  private PasswordResetProperties passwordResetProperties;

  @Test
  void shouldPersistOnlyTheHashAndReturnTheRawTokenSeparately() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");

    when(passwordResetProperties.tokenBytes()).thenReturn(32);
    when(passwordResetProperties.tokenExpiration()).thenReturn(THIRTY_MINUTES_MILLIS);
    when(resetTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

    // Act
    final ForgotPasswordTokenDetails result = service.generateToken(user);

    // Assert
    final ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(resetTokenRepository).save(captor.capture());

    final PasswordResetToken savedToken = captor.getValue();
    assertThat(savedToken.getUser()).isEqualTo(user);
    assertThat(savedToken.getSid()).isNotNull();
    assertThat(savedToken.getTokenHash()).isNotBlank();
    assertThat(savedToken.getTokenHash()).isNotEqualTo(result.token());
    assertThat(result.token()).isNotBlank();
  }

  @Test
  void shouldGenerateDifferentRawTokenOnEachCall() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");

    when(passwordResetProperties.tokenBytes()).thenReturn(32);
    when(passwordResetProperties.tokenExpiration()).thenReturn(THIRTY_MINUTES_MILLIS);
    when(resetTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

    // Act
    final ForgotPasswordTokenDetails first = service.generateToken(user);
    final ForgotPasswordTokenDetails second = service.generateToken(user);

    // Assert
    assertThat(first.token()).isNotEqualTo(second.token());
  }

  @Test
  void shouldPersistTheSha256HashOfTheReturnedRawToken() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");

    when(passwordResetProperties.tokenBytes()).thenReturn(32);
    when(passwordResetProperties.tokenExpiration()).thenReturn(THIRTY_MINUTES_MILLIS);

    final ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    when(resetTokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArguments()[0]);

    // Act
    final ForgotPasswordTokenDetails result = service.generateToken(user);

    // Assert
    final String expectedHash = sha256Base64(result.token());
    assertThat(captor.getValue().getTokenHash()).isEqualTo(expectedHash);
  }

  @Test
  void shouldDeleteAllExistingTokensForUserBeforeGeneratingNewOne() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");

    when(passwordResetProperties.tokenBytes()).thenReturn(32);
    when(passwordResetProperties.tokenExpiration()).thenReturn(THIRTY_MINUTES_MILLIS);
    when(resetTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

    // Act
    service.generateToken(user);

    // Assert
    verify(resetTokenRepository, times(1)).deleteAllFromUser(user);
  }

  @Test
  void shouldSetExpiryAccordingToConfiguredDuration() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");
    final long fifteenMinutesMillis = 900_000L;

    when(passwordResetProperties.tokenBytes()).thenReturn(32);
    when(passwordResetProperties.tokenExpiration()).thenReturn(fifteenMinutesMillis);

    final ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    when(resetTokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArguments()[0]);

    // Act
    final LocalDateTime before = LocalDateTime.now(ZoneId.of("Europe/Lisbon"));
    final ForgotPasswordTokenDetails result = service.generateToken(user);
    final LocalDateTime after = LocalDateTime.now(ZoneId.of("Europe/Lisbon"));

    // Assert - expiresAt must fall within [before + 15min, after + 15min], accounting for
    // the elapsed time of the call itself rather than asserting an exact instant
    final LocalDateTime expiresAt = captor.getValue().getExpiresAt();
    assertThat(expiresAt).isAfterOrEqualTo(before.plusMinutes(15));
    assertThat(expiresAt).isBeforeOrEqualTo(after.plusMinutes(15));
    assertThat(result.expiresIn()).isEqualTo(15L);
  }

  private String sha256Base64(final String value) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashBytes);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

}
