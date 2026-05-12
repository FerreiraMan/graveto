package me.ferreira.graveto.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import me.ferreira.graveto.identity.domain.AuthUser;
import me.ferreira.graveto.identity.domain.Role;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class LoginServiceImplTest {

  @InjectMocks
  private AuthServiceImpl service;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private JwtService jwtService;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() {
    // Arrange
    final LoginCommand command = new LoginCommand("email@test.com", "password123");
    final String expectedToken = "expected_token";
    final AuthUser dummyAuthUser = new AuthUser(UUID.randomUUID(), "email@test.com", "hash", Role.USER);
    final Authentication authenticationResponse = mock(Authentication.class);

    when(authenticationResponse.getPrincipal()).thenReturn(dummyAuthUser);
    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticationResponse);
    when(jwtService.createJwtToken(dummyAuthUser)).thenReturn(expectedToken);

    // Act
    final String actualToken = service.login(command);

    // Assert
    assertThat(actualToken).isEqualTo(expectedToken);

    final ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
    verify(authenticationManager).authenticate(authCaptor.capture());
    final Authentication capturedRequest = authCaptor.getValue();
    assertThat(capturedRequest.getPrincipal()).isEqualTo("email@test.com");
    assertThat(capturedRequest.getCredentials()).isEqualTo("password123");
  }

  @Test
  void shouldThrowExceptionWhenCredentialsAreInvalid() {
    // Arrange
    final LoginCommand command = new LoginCommand("email@test.com", "wrong-password");

    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    // Act & Assert
    assertThatThrownBy(() -> service.login(command))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Bad credentials");
    verify(jwtService, org.mockito.Mockito.never()).createJwtToken(any());
  }

}
