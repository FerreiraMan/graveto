package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.ferreira.graveto.common.web.exception.identity.UserAlreadyExistsException;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class RegisterServiceImplTest {

  @InjectMocks
  private AuthServiceImpl service;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  void shouldRegisterAndReturnUserWhenEmailIsUnique() {
    // Arrange
    final String rawPassword = "password123";
    final String encodedPassword = "$2a$10$dummyHashString...";
    final RegisterCommand command = new RegisterCommand("newuser@graveto.com", rawPassword);

    when(userRepository.fetchUserCredentials(command.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

    final User expectedSavedUser = mock(User.class);
    when(userRepository.save(any(User.class))).thenReturn(expectedSavedUser);

    // Act
    final User actualUser = service.register(command);

    // Assert
    assertThat(actualUser).isEqualTo(expectedSavedUser);

    final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    final User capturedUserToSave = userCaptor.getValue();
    assertThat(capturedUserToSave.getEmail()).isEqualTo("newuser@graveto.com");
    assertThat(capturedUserToSave.getPassword()).isEqualTo(encodedPassword);
  }

  @Test
  void shouldThrowExceptionWhenUserAlreadyExists() {
    // Arrange
    final RegisterCommand command = new RegisterCommand("duplicate@graveto.com", "password123");

    final User existingUser = mock(User.class);
    when(userRepository.fetchUserCredentials(command.email())).thenReturn(Optional.of(existingUser));

    // Act & Assert
    assertThatThrownBy(() -> service.register(command))
        .isInstanceOf(UserAlreadyExistsException.class);
    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).save(any());
  }

}
