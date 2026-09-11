package me.ferreira.graveto.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.identity.config.properties.PasswordResetProperties;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.PasswordResetTokenService;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordResetProperties passwordResetProperties;

  @Override
  @Transactional
  public ForgotPasswordTokenDetails generateToken(final User user) {

    passwordResetTokenRepository.deleteAllFromUser(user);

    final String token = generateRawToken();
    final String tokenHash = generateTokenHash(token);

    final PasswordResetToken pwResetToken =
        PasswordResetToken.create(user, tokenHash, passwordResetProperties.tokenExpiration());
    passwordResetTokenRepository.save(pwResetToken);
    return new ForgotPasswordTokenDetails(
        token,
        Duration.of(passwordResetProperties.tokenExpiration(), ChronoUnit.MILLIS).toMinutes()
    );
  }

  private String generateRawToken() {
    byte[] randomBytes = new byte[passwordResetProperties.tokenBytes()];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String generateTokenHash(final String token) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    final byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
  }

}
