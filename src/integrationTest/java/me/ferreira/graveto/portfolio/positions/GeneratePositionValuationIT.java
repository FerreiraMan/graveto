package me.ferreira.graveto.portfolio.positions;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
public class GeneratePositionValuationIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private StockExchangeService stockExchangeService;

  @Test
  void shouldReturnPositionValuationsWithCalculatedFields() {
    // Scenario: BUY 10 @ 72.50 (fee 2). Asset currentPrice = 89.45.
    // Expected: qty=10, avgCost=72.50, invested=727, marketValue=894.50, pnl=167.50, pnl%=23.04%
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset(new BigDecimal("89.45"));

    createOrder(userSid, broker, asset, OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"),
        new BigDecimal("2"));

    // Act & Assert
    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].ticker", equalTo("IWDA"))
        .body("[0].quantity", equalTo(10f))
        .body("[0].averageCost", equalTo(72.50f))
        .body("[0].totalInvested", equalTo(727.0f))
        .body("[0].currentPrice", equalTo(89.45f))
        .body("[0].marketValue", equalTo(894.5f))
        .body("[0].unrealizedPnL", equalTo(167.5f))
        .body("[0].unrealizedPnlPercent", equalTo(23.04f));
  }

  @Test
  void shouldReturnEmptyListWhenNoPositionsExist() {
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);

    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void shouldSkipPositionsWithoutCurrentPrice() {
    // Asset has no currentPrice (null) — position should be excluded from response
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset(null);

    createOrder(userSid, broker, asset, OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"),
        new BigDecimal("2"));

    given()
        .header("Authorization", "Bearer " + userSid)
        .when()
        .get("/brokers/" + broker.getSid() + "/positions")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
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
        .get("/brokers/" + broker.getSid() + "/positions")
        .then()
        .statusCode(403);
  }

  @Test
  void shouldReturnNotFoundWhenBrokerDoesNotExist() {
    given()
        .header("Authorization", "Bearer " + UUID.randomUUID())
        .when()
        .get("/brokers/" + UUID.randomUUID() + "/positions")
        .then()
        .statusCode(404);
  }

  private void createOrder(final UUID userSid, final Broker broker, final Asset asset,
                           final OrderType orderType, final BigDecimal quantity,
                           final BigDecimal price, final BigDecimal fees) {

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(), asset.getSid(), orderType, quantity, price, fees,
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

  private Asset setupAsset(final BigDecimal currentPrice) {
    final StockExchange exchange = stockExchangeService.fetchStockExchange(new FetchStockExchangeCommand(".AS"));
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    asset.setStockExchange(exchange);
    asset.setCurrentPrice(currentPrice);
    return assetRepository.save(asset);
  }

}
