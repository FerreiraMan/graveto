package me.ferreira.graveto.identity;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import me.ferreira.graveto.identity.domain.User;
import me.ferreira.graveto.identity.repository.UserRepository;
import me.ferreira.graveto.identity.web.request.RegisterRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/identity/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RegisterIT extends IdentityBaseIntegrationTest {

  private static final String VALID_EMAIL = "newuser@graveto.com";
  private static final String VALID_PASSWORD = "SuperSecretPassword123";

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void shouldReturn201AndLocationHeaderWhenRegistrationIsSuccessful() {
    // Arrange
    final RegisterRequestDto request = new RegisterRequestDto(VALID_EMAIL, VALID_PASSWORD);

    // Act
    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/auth/register")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("email", equalTo(VALID_EMAIL));

    // Assert
    final User savedUser = userRepository.fetchUserCredentials(VALID_EMAIL).get();

    assertThat(savedUser.getEmail()).isEqualTo(VALID_EMAIL);
    assertThat(savedUser.getPassword()).isNotEqualTo(VALID_PASSWORD);
    assertThat(passwordEncoder.matches(VALID_PASSWORD, savedUser.getPassword())).isTrue();
  }

}
