package me.ferreira.graveto.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.identity.config.properties.PasswordResetProperties;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.PasswordResetTokenService;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

  private static final String HASHING_ALGORITHM = "SHA-256";

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

  @Override
  @Transactional(readOnly = true)
  public Optional<User> validateToken(final String token) {

    final String providedTokenHash = generateTokenHash(token);
    final Optional<PasswordResetToken> passwordResetTokenOpt =
        passwordResetTokenRepository.findByTokenHash(providedTokenHash);

    if (passwordResetTokenOpt.isEmpty()) {
      return Optional.empty();
    }
    final PasswordResetToken passwordResetToken = passwordResetTokenOpt.get();

    if (!passwordResetToken.isExpired()) {
      return Optional.of(passwordResetToken.getUser());
    }

    return Optional.empty();
  }

  @Override
  @Transactional
  public void invalidateToken(final User user) {
    passwordResetTokenRepository.deleteAllFromUser(user);
  }

  private String generateRawToken() {
    byte[] randomBytes = new byte[passwordResetProperties.tokenBytes()];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String generateTokenHash(final String token) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASHING_ALGORITHM);
    } catch (final NoSuchAlgorithmException e) {
      log.error("Error with the server algorithm definition: [{}}]", HASHING_ALGORITHM);
      throw new IllegalStateException(e.getMessage());
    }
    final byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
  }

}
