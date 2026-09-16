package me.ferreira.graveto.identity.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.impl.PasswordResetTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InvalidatePasswordResetTokenServiceImplTest {

  @InjectMocks
  private PasswordResetTokenServiceImpl service;
  @Mock
  private PasswordResetTokenRepository resetTokenRepository;

  @Test
  void shouldInvalidateAllTokensForUser() {
    // Arrange
    final User user = User.create("test@graveto.com", "hash");

    // Act
    service.invalidateToken(user);

    // Assert
    verify(resetTokenRepository, times(1)).deleteAllFromUser(user);
  }

}
