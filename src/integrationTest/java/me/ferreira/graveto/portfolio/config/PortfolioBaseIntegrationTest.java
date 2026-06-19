package me.ferreira.graveto.portfolio.config;

import io.restassured.RestAssured;
import me.ferreira.graveto.portfolio.config.security.PortfolioTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest(
    module = "portfolio",
    mode = ApplicationModuleTest.BootstrapMode.STANDALONE,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import({PortfolioIntegrationTestConfiguration.class, PortfolioTestSecurityConfig.class})
public class PortfolioBaseIntegrationTest {

  @LocalServerPort
  private int port;

  @BeforeEach
  void setupRestAssured() {
    RestAssured.port = this.port;
    RestAssured.basePath = "/api";
  }

}
