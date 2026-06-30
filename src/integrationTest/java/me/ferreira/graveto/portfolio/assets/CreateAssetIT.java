package me.ferreira.graveto.portfolio.assets;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.web.dto.request.CreateAssetRequestDto;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateAssetIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private MockServerClient mockServerClient;

  @AfterEach
  void resetMockServer() {
    mockServerClient.reset();
  }

  @Test
  void shouldCreateAssetAndEnrichWithCurrentPrice() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "IWDA.AS", "iShares Core MSCI World UCITS ETF", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote")
            .withQueryStringParameter("symbols", "IWDA.AS"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[
                  {"symbol":"IWDA.AS","longName":"iShares Core MSCI World","quoteType":"ETF","regularMarketPrice":89.45,"currency":"EUR"}
                ]}}
                """));

    // Act
    final String assetSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/assets")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("ticker", equalTo("IWDA"))
        .body("name", equalTo("iShares Core MSCI World UCITS ETF"))
        .body("type", equalTo(AssetType.ETF.name()))
        .body("currency", equalTo(Currency.EUR.name()))
        .body("exchangeLocation", equalTo("Netherlands (the)"))
        .body("exchangeName", equalTo("Euronext Amsterdam"))
        .extract()
        .path("sid");

    // Assert — enrichment happens after commit, poll until price is set
    final Optional<Asset> enrichedAsset = assetRepository.findBySid(UUID.fromString(assetSid));
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      assertThat(enrichedAsset).isPresent();
      assertThat(enrichedAsset.get().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));
    });
  }

  @Test
  void shouldCreateAssetWithoutPriceWhenQuoteApiReturnsNoMatch() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "IWDA.AS", "iShares Core MSCI World UCITS ETF", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote")
            .withQueryStringParameter("symbols", "IWDA.AS"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[]}}
                """));

    // Act
    final String assetSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/assets")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");

    // Assert — asset exists but price is null
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      final Optional<Asset> savedAsset = assetRepository.findBySid(UUID.fromString(assetSid));
      assertThat(savedAsset).isPresent();
      assertThat(savedAsset.get().getTicker()).isEqualTo("IWDA");
      assertThat(savedAsset.get().getCurrentPrice()).isNull();
    });
  }

  @Test
  void shouldReturnExistingAssetIfAlreadyCreated() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "IWDA.AS", "iShares Core MSCI World UCITS ETF", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote")
            .withQueryStringParameter("symbols", "IWDA.AS"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[
                  {"symbol":"IWDA.AS","longName":"iShares Core MSCI World","quoteType":"ETF","regularMarketPrice":89.45,"currency":"EUR"}
                ]}}
                """));

    // Create first
    final String firstSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/assets")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");

    // Act — create same asset again
    final String secondSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/assets")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");

    // Assert — same asset returned, no duplicate
    assertThat(firstSid).isEqualTo(secondSid);
  }

  @Test
  void shouldReturnBadRequestForSymbolWithoutExchangeSuffix() {
    // Arrange
    final CreateAssetRequestDto request = new CreateAssetRequestDto(
        "AAPL", "Apple Inc.", AssetType.EQUITY, Currency.USD);

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + UUID.randomUUID())
        .body(request)
        .when()
        .post("/assets")
        .then()
        .statusCode(400);
  }

}
