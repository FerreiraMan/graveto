package me.ferreira.graveto.identity;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import io.restassured.http.ContentType;
import java.time.Duration;
import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenRepository;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.ForgotPasswordRequestDto;
import me.ferreira.graveto.identity.web.request.ResetPasswordRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@TestPropertySource(properties = "management.health.mail.enabled=false")
@Sql(scripts = {"/identity/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ResetPasswordIT extends IdentityBaseIntegrationTest {

  private static final String VALID_EMAIL = "integration@graveto.com";
  private static final String ORIGINAL_PASSWORD = "Password123";
  private static final String NEW_PASSWORD = "NewPassword456";

  @Autowired
  private AuthService authService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @MockitoBean
  private JavaMailSender mailSender;

  @BeforeEach
  void setUpTestUser() {
    authService.register(new RegisterCommand(VALID_EMAIL, ORIGINAL_PASSWORD));
  }

  @Test
  void shouldResetPasswordAndInvalidateTokenWhenTokenIsValid() {
    // Arrange
    final String rawToken = requestResetAndCaptureRawToken();

    // Act
    given()
        .contentType(ContentType.JSON)
        .body(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
        .when()
        .patch("/auth/reset-password")
        .then()
        .statusCode(204);

    // Assert
    final User user = userRepository.fetchUserCredentials(VALID_EMAIL).orElseThrow();
    assertThat(passwordEncoder.matches(NEW_PASSWORD, user.getPassword())).isTrue();
    assertThat(passwordResetTokenRepository.findAllByUser(user)).isEmpty();
  }

  @Test
  void shouldRejectReusedTokenAfterSuccessfulReset() {
    // Arrange
    final String rawToken = requestResetAndCaptureRawToken();

    given()
        .contentType(ContentType.JSON)
        .body(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
        .when()
        .patch("/auth/reset-password")
        .then()
        .statusCode(204);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .body(new ResetPasswordRequestDto(rawToken, "AnotherPassword789"))
        .when()
        .patch("/auth/reset-password")
        .then()
        .statusCode(401);
  }

  @Test
  void shouldRejectUnknownToken() {
    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .body(new ResetPasswordRequestDto("non-existent-token", NEW_PASSWORD))
        .when()
        .patch("/auth/reset-password")
        .then()
        .statusCode(401);
  }

  private String requestResetAndCaptureRawToken() {
    given()
        .contentType(ContentType.JSON)
        .body(new ForgotPasswordRequestDto(VALID_EMAIL))
        .when()
        .post("/auth/forgot-password")
        .then()
        .statusCode(202);

    final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(mailSender).send(captor.capture()));

    final String emailBody = captor.getValue().getText();
    return emailBody.split("\n")[1].trim();
  }

}
