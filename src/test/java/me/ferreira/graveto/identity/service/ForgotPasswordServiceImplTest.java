package me.ferreira.graveto.identity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.ferreira.graveto.common.mail.domain.event.PasswordResetEmailEvent;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.service.command.ForgotPasswordCommand;
import me.ferreira.graveto.identity.service.impl.AuthServiceImpl;
import me.ferreira.graveto.identity.service.payload.ForgotPasswordTokenDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
public class ForgotPasswordServiceImplTest {

  @InjectMocks
  private AuthServiceImpl service;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private JwtService jwtService;
  @Mock
  private PasswordResetTokenService passwordResetTokenService;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private UserRepository userRepository;

  @Test
  void shouldGenerateTokenAndPublishEventWhenUserExists() {
    // Arrange
    final ForgotPasswordCommand command = new ForgotPasswordCommand("email@test.com");
    final User user = User.create("email@test.com", "hash");
    final ForgotPasswordTokenDetails tokenDetails = new ForgotPasswordTokenDetails("raw-token", 30L);

    when(userRepository.fetchUserCredentials("email@test.com")).thenReturn(Optional.of(user));
    when(passwordResetTokenService.generateToken(user)).thenReturn(tokenDetails);

    // Act
    service.forgotPassword(command);

    // Assert
    verify(passwordResetTokenService, times(1)).generateToken(user);
    verify(eventPublisher, times(1))
        .publishEvent(new PasswordResetEmailEvent("email@test.com", "raw-token", 30L));
  }

  @Test
  void shouldDoNothingWhenUserDoesNotExist() {
    // Arrange
    final ForgotPasswordCommand command = new ForgotPasswordCommand("missing@test.com");
    when(userRepository.fetchUserCredentials("missing@test.com")).thenReturn(Optional.empty());

    // Act
    service.forgotPassword(command);

    // Assert
    verify(passwordResetTokenService, never()).generateToken(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldLookUpUserByCommandEmail() {
    // Arrange
    final ForgotPasswordCommand command = new ForgotPasswordCommand("email@test.com");
    when(userRepository.fetchUserCredentials("email@test.com")).thenReturn(Optional.empty());

    // Act
    service.forgotPassword(command);

    // Assert
    verify(userRepository, times(1)).fetchUserCredentials("email@test.com");
  }

}
