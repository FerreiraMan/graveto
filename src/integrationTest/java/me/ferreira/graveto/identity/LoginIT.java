package me.ferreira.graveto.identity;

import io.restassured.http.ContentType;
import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import me.ferreira.graveto.identity.service.AuthService;
import me.ferreira.graveto.identity.service.command.RegisterCommand;
import me.ferreira.graveto.identity.web.request.LoginRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@Sql(scripts = {"/identity/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class LoginIT extends IdentityBaseIntegrationTest {

    private static final String VALID_EMAIL = "integration@graveto.com";
    private static final String VALID_PASSWORD = "Password123";

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUpTestUser() {
        authService.register(new RegisterCommand(VALID_EMAIL, VALID_PASSWORD));
    }

    @Test
    void shouldReturn200AndJwtTokenWhenCredentialsAreValid() {
        // Arrange
        final LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, VALID_PASSWORD);

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    void shouldReturnForbiddenWhenPasswordIsIncorrect() {
        // Arrange
        final LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, "WrongPassword");

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(403);
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotExist() {
        // Arrange
        final LoginRequestDTO request = new LoginRequestDTO("ghost@graveto.com", "SomePassword");

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(403);
    }

}
