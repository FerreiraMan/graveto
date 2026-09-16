package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.impl.PasswordResetTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidatePasswordResetTokenServiceImplTest {

  private static final long THIRTY_MINUTES_MILLIS = 1_800_000L;
  private static final long EXPIRED_ONE_MINUTE_AGO_MILLIS = -60_000L;

  @InjectMocks
  private PasswordResetTokenServiceImpl service;
  @Mock
  private PasswordResetTokenRepository resetTokenRepository;

  @Test
  void shouldReturnEmptyIfNoTokenHashMatchIsFound() {
    // Arrange
    when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    // Act
    final Optional<User> result = service.validateToken("token");

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyIfExistingTokenIsExpired() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");
    final PasswordResetToken passwordResetToken =
        PasswordResetToken.create(user, "any-stored-hash", EXPIRED_ONE_MINUTE_AGO_MILLIS);

    when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(passwordResetToken));

    // Act
    final Optional<User> result = service.validateToken("token");

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnUserIfTokenExistsAndIsNotExpired() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");
    final PasswordResetToken passwordResetToken =
        PasswordResetToken.create(user, "any-stored-hash", THIRTY_MINUTES_MILLIS);

    when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(passwordResetToken));

    // Act
    final Optional<User> result = service.validateToken("token");

    // Assert
    assertThat(result).isNotEmpty();
    assertThat(result.get().getEmail()).isEqualTo("test@graveto.com");
  }

}
