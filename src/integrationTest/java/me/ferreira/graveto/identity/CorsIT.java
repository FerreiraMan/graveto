package me.ferreira.graveto.identity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import me.ferreira.graveto.identity.config.IdentityBaseIntegrationTest;
import org.junit.jupiter.api.Test;

public class CorsIT extends IdentityBaseIntegrationTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:5173";
  private static final String DISALLOWED_ORIGIN = "http://hacker.com";

  @Test
  void shouldAcceptPreflightAndReturnsAllowedOriginHeader() {
    given()
        .header("Origin", ALLOWED_ORIGIN)
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/auth/login")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo(ALLOWED_ORIGIN))
        .header("Access-Control-Allow-Methods", containsString("POST"));
  }

  @Test
  void shouldRejectPreflightAndOmitsCorsHeaders() {
    given()
        .header("Origin", DISALLOWED_ORIGIN)
        .header("Access-Control-Request-Method", "POST")
        .when()
        .options("/auth/login")
        .then()
        .statusCode(403)
        .header("Access-Control-Allow-Origin", nullValue());
  }

}
