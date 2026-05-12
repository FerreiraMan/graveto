package me.ferreira.graveto.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.command.LoginCommand;
import me.ferreira.graveto.identity.web.request.LoginRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
public class LoginControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AuthService service;
  @MockitoBean
  private JwtService jwtService;

  private static Stream<Arguments> invalidLoginRequests() {
    return Stream.of(
        Arguments.of(new LoginRequestDto("  ", "password"), "email"),
        Arguments.of(new LoginRequestDto("", "password"), "email"),
        Arguments.of(new LoginRequestDto(null, "password"), "email"),
        Arguments.of(new LoginRequestDto("email", "   "), "password"),
        Arguments.of(new LoginRequestDto("email", ""), "password"),
        Arguments.of(new LoginRequestDto("email", null), "password")
    );
  }

  @Test
  void shouldReturnOkWithTokenWhenLoginIsSuccessful() {
    // Arrange
    final LoginRequestDto request = new LoginRequestDto("test@graveto.com", "password123");
    final String expectedToken = "token_example";

    when(service.login(any(LoginCommand.class))).thenReturn(expectedToken);

    // Act & Assert
    final MvcTestResult testResult = mvc.post()
        .uri("/auth/login")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.token").asString().isEqualTo(expectedToken);
  }

  @Test
  void shouldTrimAndLowerCaseEmailBeforePassingToService() {
    // Arrange
    final LoginRequestDto request = new LoginRequestDto("  WeIrDCaSe@GraVeTo.com  ", "password123");
    when(service.login(any(LoginCommand.class))).thenReturn("mock-token");

    // Act
    mvc.post()
        .uri("/auth/login")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    final ArgumentCaptor<LoginCommand> commandCaptor = ArgumentCaptor.forClass(LoginCommand.class);
    verify(service).login(commandCaptor.capture());
    final LoginCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.email()).isEqualTo("weirdcase@graveto.com");
    assertThat(capturedCommand.password()).isEqualTo("password123");
  }

  @ParameterizedTest
  @MethodSource("invalidLoginRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnLoginRequest(
      final LoginRequestDto request,
      final String expectedErrorField) {

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/auth/login")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

}
