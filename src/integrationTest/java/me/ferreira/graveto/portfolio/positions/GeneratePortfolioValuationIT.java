package me.ferreira.graveto.portfolio.positions;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import me.ferreira.graveto.portfolio.utils.BrokerTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class GeneratePortfolioValuationIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private StockExchangeService stockExchangeService;

  @Test
  void shouldReturnPortfolioSummaryWithCalculatedFields() {
    // Scenario: Two BUY orders on different assets:
    //   IWDA: 10 @ 50, fee 0 → invested=500, currentPrice=60, marketValue=600, pnl=100
    //   VWCE: 10 @ 80, fee 0 → invested=800, currentPrice=100, marketValue=1000, pnl=200
    // Expected totals: invested=1300, marketValue=1600, pnl=300, pnl%=23.08
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset iwda = setupAsset("IWDA", new BigDecimal("60"));
    final Asset vwce = setupAsset("VWCE", new BigDecimal("100"));

    createOrder(userSid, broker, iwda, new BigDecimal("10"), new BigDecimal("50"), BigDecimal.ZERO);
    createOrder(userSid, broker, vwce, new BigDecimal("10"), new BigDecimal("80"), BigDecimal.ZERO);

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions/summary")
        .then()
        .statusCode(200)
        .body("totalInvested", equalTo(1300f))
        .body("totalMarketValue", equalTo(1600f))
        .body("totalUnrealizedPnL", equalTo(300f));
  }

  @Test
  void shouldReturnZeroedSummaryWhenNoPositionsExist() {
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);

    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions/summary")
        .then()
        .statusCode(200)
        .body("totalInvested", equalTo(0))
        .body("totalMarketValue", equalTo(0))
        .body("totalUnrealizedPnL", equalTo(0))
        .body("totalUnrealizedPnlPercent", equalTo(0));
  }

  @Test
  void shouldReturnZeroedSummaryWhenAssetsHaveNoPrice() {
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset("IWDA", null);

    createOrder(userSid, broker, asset, new BigDecimal("10"), new BigDecimal("50"), BigDecimal.ZERO);

    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions/summary")
        .then()
        .statusCode(200)
        .body("totalInvested", equalTo(0))
        .body("totalMarketValue", equalTo(0));
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfBroker() {
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(ownerSid, new UserResponseDto(ownerSid, "owner@example.com")));

    final Broker broker = setupBroker(ownerSid);

    given()
        .header("Authorization", "Bearer " + otherUserSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions/summary")
        .then()
        .statusCode(403);
  }

  @Test
  void shouldReturnNotFoundWhenBrokerDoesNotExist() {
    given()
        .header("Authorization", "Bearer " + UUID.randomUUID())
        .when()
        .get("/brokers/" + UUID.randomUUID() + "/positions/summary")
        .then()
        .statusCode(404);
  }

  private void createOrder(final UUID userSid, final Broker broker, final Asset asset,
                           final BigDecimal quantity, final BigDecimal price, final BigDecimal fees) {

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(), asset.getSid(), OrderType.BUY, quantity, price, fees,
        Currency.EUR, LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(201);
  }

  private Broker setupBroker(final UUID ownerSid) {
    final Broker broker = BrokerTestFactory.createBrokerWithOwner(ownerSid, "DEGIRO", null);
    return brokerRepository.save(broker);
  }

  private Asset setupAsset(final String ticker, final BigDecimal currentPrice) {
    final StockExchange exchange = stockExchangeService.fetchStockExchange(new FetchStockExchangeCommand(".AS"));
    final Asset asset = Asset.create(ticker, "Test " + ticker, AssetType.ETF, Currency.EUR);
    asset.setStockExchange(exchange);
    asset.setCurrentPrice(currentPrice);
    return assetRepository.save(asset);
  }

}
