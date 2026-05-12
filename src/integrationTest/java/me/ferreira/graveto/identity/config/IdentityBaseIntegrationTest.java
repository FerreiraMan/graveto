package me.ferreira.graveto.identity.config;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest(
    module = "identity",
    mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(IdentityIntegrationTestConfiguration.class)
public abstract class IdentityBaseIntegrationTest {

  @LocalServerPort
  private int port;

  @BeforeEach
  void setupRestAssured() {
    RestAssured.port = this.port;
    RestAssured.basePath = "/api";
  }

}
