package me.ferreira.graveto.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import me.ferreira.graveto.common.web.exception.identity.InvalidResetPasswordTokenException;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.JwtService;
import me.ferreira.graveto.identity.service.command.ResetPasswordCommand;
import me.ferreira.graveto.identity.web.request.ResetPasswordRequestDto;
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
public class ResetPasswordControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AuthService service;
  @MockitoBean
  private JwtService jwtService;

  private static Stream<Arguments> invalidResetPasswordRequests() {
    return Stream.of(
        Arguments.of(new ResetPasswordRequestDto("  ", "password"), "token"),
        Arguments.of(new ResetPasswordRequestDto("", "password"), "token"),
        Arguments.of(new ResetPasswordRequestDto(null, "password"), "token"),
        Arguments.of(new ResetPasswordRequestDto("token", "   "), "newPassword"),
        Arguments.of(new ResetPasswordRequestDto("token", ""), "newPassword"),
        Arguments.of(new ResetPasswordRequestDto("token", null), "newPassword"),
        Arguments.of(new ResetPasswordRequestDto("token", "pwd"), "newPassword")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidResetPasswordRequests")
  void shouldReturnBadRequestForInvalidPayloadsOnResetPasswordRequest(
      final ResetPasswordRequestDto request,
      final String expectedErrorField) {

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/auth/reset-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldResetPasswordSuccessfully() {
    // Arrange
    final ResetPasswordRequestDto request = new ResetPasswordRequestDto("token", "newPassword");

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/auth/reset-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.NO_CONTENT);

    final ArgumentCaptor<ResetPasswordCommand> commandCaptor = ArgumentCaptor.forClass(ResetPasswordCommand.class);
    verify(service).resetPassword(commandCaptor.capture());
    assertThat(commandCaptor.getValue().token()).isEqualTo("token");
    assertThat(commandCaptor.getValue().newPassword()).isEqualTo("newPassword");
  }

  @Test
  void shouldReturnUnauthorizedWithGenericMessageWhenTokenIsInvalidOrExpired() {
    // Arrange
    final ResetPasswordRequestDto request = new ResetPasswordRequestDto("token", "newPassword");
    doThrow(new InvalidResetPasswordTokenException("Unable to process password reset request."))
        .when(service).resetPassword(any(ResetPasswordCommand.class));

    // Act
    final MvcTestResult testResult = mvc.patch()
        .uri("/auth/reset-password")
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult)
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .bodyJson()
        .extractingPath("$.detail").asString()
        .isEqualTo("Unable to process password reset request.");
  }

}
