package me.ferreira.graveto.identity;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.List;
import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import me.ferreira.graveto.identity.domain.PasswordResetToken;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.repository.passwordresettoken.PasswordResetTokenJpaRepository;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.ForgotPasswordRequestDto;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@TestPropertySource(properties = "management.health.mail.enabled=false")
@Sql(scripts = {"/identity/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ForgotPasswordIT extends IdentityBaseIntegrationTest {

  private static final String VALID_EMAIL = "integration@graveto.com";
  private static final String VALID_PASSWORD = "Password123";

  @Autowired
  private AuthService authService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordResetTokenJpaRepository passwordResetTokenJpaRepository;
  @MockitoBean
  private JavaMailSender mailSender;

  @BeforeEach
  void setUpTestUser() {
    authService.register(new RegisterCommand(VALID_EMAIL, VALID_PASSWORD));
  }

  @Test
  void shouldPersistHashedTokenAndSendEmailWhenAccountExists() {
    // Arrange
    final ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(VALID_EMAIL);

    // Act
    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/auth/forgot-password")
        .then()
        .statusCode(202)
        .body("message", Matchers.notNullValue());

    // Assert
    final User user = userRepository.fetchUserCredentials(VALID_EMAIL).get();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      final List<PasswordResetToken> tokens = passwordResetTokenJpaRepository.findAll().stream()
          .filter(token -> token.getUser().getId().equals(user.getId()))
          .toList();

      assertThat(tokens).hasSize(1);
      assertThat(tokens.get(0).getTokenHash()).isNotBlank();
      assertThat(tokens.get(0).getExpiresAt()).isNotNull();
    });

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
      verify(mailSender).send(captor.capture());

      final SimpleMailMessage sentMessage = captor.getValue();
      assertThat(sentMessage.getTo()).containsExactly(VALID_EMAIL);

      final PasswordResetToken persistedToken = passwordResetTokenJpaRepository.findAll().stream()
          .filter(token -> token.getUser().getId().equals(user.getId()))
          .findFirst().orElseThrow();
      assertThat(sentMessage.getText()).doesNotContain(persistedToken.getTokenHash());
    });
  }

  @Test
  void shouldReturnSameResponseWhenAccountDoesNotExist() {
    // Arrange
    final ForgotPasswordRequestDto existingEmailRequest = new ForgotPasswordRequestDto(VALID_EMAIL);
    final ForgotPasswordRequestDto missingEmailRequest = new ForgotPasswordRequestDto("ghost@graveto.com");

    // Act
    final String existingEmailBody = given()
        .contentType(ContentType.JSON)
        .body(existingEmailRequest)
        .when()
        .post("/auth/forgot-password")
        .then()
        .statusCode(202)
        .extract().body().asString();

    final String missingEmailBody = given()
        .contentType(ContentType.JSON)
        .body(missingEmailRequest)
        .when()
        .post("/auth/forgot-password")
        .then()
        .statusCode(202)
        .extract().body().asString();

    // Assert
    assertThat(missingEmailBody).isEqualTo(existingEmailBody);

    // Assert
    final long tokenCount = passwordResetTokenJpaRepository.findAll().stream()
        .filter(token -> "ghost@graveto.com".equals(token.getUser().getEmail()))
        .count();
    assertThat(tokenCount).isZero();
  }

  @Test
  void shouldInvalidatePreviousTokenWhenRequestedAgain() {
    // Arrange
    final ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(VALID_EMAIL);
    final User user = userRepository.fetchUserCredentials(VALID_EMAIL).get();

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/auth/forgot-password");

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        assertThat(passwordResetTokenJpaRepository.findAll().stream()
            .filter(token -> token.getUser().getId().equals(user.getId()))
            .toList()).hasSize(1));

    final String firstTokenHash = passwordResetTokenJpaRepository.findAll().stream()
        .filter(token -> token.getUser().getId().equals(user.getId()))
        .findFirst().orElseThrow().getTokenHash();

    // Act
    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/auth/forgot-password");

    // Assert
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      final List<PasswordResetToken> tokens = passwordResetTokenJpaRepository.findAll().stream()
          .filter(token -> token.getUser().getId().equals(user.getId()))
          .toList();

      assertThat(tokens).hasSize(1);
      assertThat(tokens.get(0).getTokenHash()).isNotEqualTo(firstTokenHash);
    });
  }

}
