package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.ferreira.graveto.common.web.exception.ApplicationException;
import me.ferreira.graveto.common.web.exception.identity.InvalidResetPasswordTokenException;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.service.command.ResetPasswordCommand;
import me.ferreira.graveto.identity.service.impl.AuthServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordServiceImplTest {

  @InjectMocks
  private AuthServiceImpl service;
  @Mock
  private PasswordResetTokenService passwordResetTokenService;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private UserRepository userRepository;

  @Test
  void shouldThrowExceptionWhenTokenIsNotValid() {
    // Arrange
    final ResetPasswordCommand command = new ResetPasswordCommand("token", "newPassword");

    when(passwordResetTokenService.validateToken(command.token())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.resetPassword(command))
        .isInstanceOf(InvalidResetPasswordTokenException.class)
        .satisfies(ex -> {
          final ApplicationException ae = (ApplicationException) ex;
          Assertions.assertThat(ae.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
          Assertions.assertThat(ae.getSafeMessage()).isEqualTo("Unable to process password reset request.");
        });
  }

  @Test
  void shouldResetPassword() {
    // Arrange
    final User user = User.create("email@test.com", "hash");
    final String rawNewPassword = "password123";
    final String encodedNewPassword = "$2a$10$dummyHashString...";
    final ResetPasswordCommand command = new ResetPasswordCommand("token", rawNewPassword);

    when(passwordResetTokenService.validateToken(command.token())).thenReturn(Optional.of(user));
    when(passwordEncoder.encode(rawNewPassword)).thenReturn(encodedNewPassword);

    // Act
    service.resetPassword(command);

    // Assert

    final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    final User capturedUserToSave = userCaptor.getValue();
    assertThat(capturedUserToSave.getEmail()).isEqualTo("email@test.com");
    assertThat(capturedUserToSave.getPassword()).isEqualTo(encodedNewPassword);
    verify(passwordResetTokenService, times(1)).invalidateToken(user);
  }

}
