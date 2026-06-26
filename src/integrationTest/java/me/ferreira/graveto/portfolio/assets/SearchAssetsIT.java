package me.ferreira.graveto.portfolio.assets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.restassured.http.ContentType;
import java.util.UUID;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SearchAssetsIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private MockServerClient mockServerClient;

  @AfterEach
  void resetMockServer() {
    mockServerClient.reset();
  }

  @Test
  void shouldSearchAssets() {

    // Arrange
    createExpectation();
    final UUID userSid = UUID.randomUUID();

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .queryParam("keyword", "IWDA")
        .when()
        .get("/assets/search")
        .then()
        .statusCode(200)
        .body("size()", equalTo(2))
        .body("[0].ticker", equalTo("IWDA.AS"))
        .body("[0].name", equalTo("iShares Core MSCI World"))
        .body("[0].type", equalTo("ETF"))
        .body("[0].exchange", equalTo("Amsterdam"))
        .body("[1].ticker", equalTo("IWDA.L"))
        .body("[1].exchange", equalTo("London"));
  }

  private void createExpectation() {

    final String responseBody = """
        {"ResultSet":{"Query":"iwda","Result":[
          {"symbol":"IWDA.AS","name":"iShares Core MSCI World","exch":"AMS","type":"E","exchDisp":"Amsterdam","typeDisp":"ETF"},
          {"symbol":"IWDA.L","name":"iShares Core MSCI World","exch":"LSE","type":"E","exchDisp":"London","typeDisp":"ETF"}
        ]}}
        """;

    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/v6/finance/autocomplete")
                .withQueryStringParameter("query", "iwda"))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(responseBody)
        );
  }

}
