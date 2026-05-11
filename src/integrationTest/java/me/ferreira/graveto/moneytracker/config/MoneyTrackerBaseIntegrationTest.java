package me.ferreira.graveto.moneytracker.config;

import io.restassured.RestAssured;
import me.ferreira.graveto.moneytracker.config.security.MoneyTrackerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest(
    module = "moneytracker",
    mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import({MoneyTrackerIntegrationTestConfiguration.class, MoneyTrackerTestSecurityConfig.class})
public abstract class MoneyTrackerBaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setupRestAssured() {
        RestAssured.port = this.port;
        RestAssured.basePath = "/api";
    }

}
