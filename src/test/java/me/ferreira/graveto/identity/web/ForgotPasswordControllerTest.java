package me.ferreira.graveto.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.command.ForgotPasswordCommand;
import me.ferreira.graveto.identity.web.request.ForgotPasswordRequestDto;
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
public class ForgotPasswordControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AuthService service;
  @MockitoBean
  private JwtService jwtService;

  private static Stream<Arguments> invalidForgotPasswordRequests() {
    return Stream.of(
        Arguments.of(new ForgotPasswordRequestDto("  "), "email"),
        Arguments.of(new ForgotPasswordRequestDto(""), "email"),
        Arguments.of(new ForgotPasswordRequestDto(null), "email"),
        Arguments.of(new ForgotPasswordRequestDto("not-an-email"), "email"),
        Arguments.of(new ForgotPasswordRequestDto("  valid-with-whitespace@email.com "), "email")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidForgotPasswordRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnForgotPasswordRequest(
      final ForgotPasswordRequestDto request,
      final String expectedErrorField) {

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/auth/forgot-password")
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

  @Test
  void shouldReturnOkWithGenericMessageWhenEmailExists() {
    // Arrange
    final ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@graveto.com");

    // Act & Assert
    final MvcTestResult testResult = mvc.post()
        .uri("/auth/forgot-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.ACCEPTED)
        .bodyJson()
        .extractingPath("$.message").asString().isNotBlank();

    verify(service).forgotPassword(any(ForgotPasswordCommand.class));
  }

  @Test
  void shouldReturnSameResponseWhenEmailDoesNotExist() {
    // Arrange
    final ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("missing@graveto.com");

    // Act
    final MvcTestResult existingEmailResult = mvc.post()
        .uri("/auth/forgot-password")
        .content(objectMapper.writeValueAsString(new ForgotPasswordRequestDto("test@graveto.com")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    final MvcTestResult missingEmailResult = mvc.post()
        .uri("/auth/forgot-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert

    assertThat(existingEmailResult).hasStatus(HttpStatus.ACCEPTED);
    assertThat(missingEmailResult).hasStatus(HttpStatus.ACCEPTED);
    assertThat(existingEmailResult).bodyJson().extractingPath("$.message").asString()
        .isEqualTo("If an account exists for that email address, a password reset code has been sent.");
    assertThat(missingEmailResult).bodyJson().extractingPath("$.message").asString()
        .isEqualTo("If an account exists for that email address, a password reset code has been sent.");
  }

  @Test
  void shouldTrimAndLowerCaseEmailBeforePassingToService() {
    // Arrange
    final ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("WeIrDCaSe@GraVeTo.com");

    // Act
    mvc.post()
        .uri("/auth/forgot-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    final ArgumentCaptor<ForgotPasswordCommand> commandCaptor = ArgumentCaptor.forClass(ForgotPasswordCommand.class);
    verify(service).forgotPassword(commandCaptor.capture());
    assertThat(commandCaptor.getValue().email()).isEqualTo("weirdcase@graveto.com");
  }

}
