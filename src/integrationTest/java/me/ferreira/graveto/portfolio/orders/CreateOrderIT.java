package me.ferreira.graveto.portfolio.orders;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import me.ferreira.graveto.portfolio.utils.BrokerTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateOrderIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private StockExchangeService stockExchangeService;

  @Test
  void shouldCreateOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();
    final LocalDateTime executedAt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS);

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(),
        asset.getSid(),
        OrderType.BUY,
        new BigDecimal("90"),
        new BigDecimal("72.50"),
        new BigDecimal("2.00"),
        Currency.EUR,
        executedAt,
        "first buy"
    );

    // Act
    final String orderSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("sid", notNullValue())
        .body("broker.sid", equalTo(broker.getSid().toString()))
        .body("broker.name", equalTo(broker.getName()))
        .body("asset.sid", equalTo(asset.getSid().toString()))
        .body("asset.name", equalTo(asset.getTicker()))
        .body("orderType", equalTo("BUY"))
        .body("notes", equalTo("first buy"))
        .extract()
        .path("sid");

    // Assert
    assertPersistedOrder(orderSid, userSid, broker, asset, OrderType.BUY,
        new BigDecimal("90"), new BigDecimal("72.50"), new BigDecimal("2.00"),
        Currency.EUR, executedAt, "first buy");
  }

  @Test
  void shouldCreateOrderWithDefaultFeesAndExecutedAt() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(),
        asset.getSid(),
        OrderType.BUY,
        new BigDecimal("10"),
        new BigDecimal("72.50"),
        null,
        Currency.EUR,
        null,
        null
    );

    // Act
    final String orderSid = given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(201)
        .body("sid", notNullValue())
        .extract()
        .path("sid");

    // Assert
    final Order savedOrder = orderRepository.findBySid(UUID.fromString(orderSid)).get();
    assertThat(savedOrder.getFees()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(savedOrder.getExecutedAt()).isNotNull();
    assertThat(savedOrder.getExecutedAt().truncatedTo(ChronoUnit.MINUTES))
        .isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    assertThat(savedOrder.getNotes()).isNull();
  }

  @Test
  void shouldReturnForbiddenWhenUserIsNotMemberOfBroker() {
    // Arrange
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(ownerSid, new UserResponseDto(ownerSid, "owner@example.com")));

    final Broker broker = setupBroker(ownerSid);
    final Asset asset = setupAsset();

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(),
        asset.getSid(),
        OrderType.BUY,
        new BigDecimal("10"),
        new BigDecimal("72.50"),
        null,
        Currency.EUR,
        null,
        null
    );

    // Act & Assert
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(403);
  }

  private void assertPersistedOrder(final String orderSid, final UUID userSid, final Broker broker, final Asset asset,
                                    final OrderType orderType, final BigDecimal quantity, final BigDecimal price,
                                    final BigDecimal fees, final Currency currency, final LocalDateTime executedAt,
                                    final String notes) {

    final Order savedOrder = orderRepository.findBySid(UUID.fromString(orderSid)).orElseThrow();

    assertThat(savedOrder.getSid()).isEqualTo(UUID.fromString(orderSid));
    assertThat(savedOrder.getUserSid()).isEqualTo(userSid);
    assertThat(savedOrder.getBroker().getSid()).isEqualTo(broker.getSid());
    assertThat(savedOrder.getAsset().getSid()).isEqualTo(asset.getSid());
    assertThat(savedOrder.getOrderType()).isEqualTo(orderType);
    assertThat(savedOrder.getQuantity()).isEqualByComparingTo(quantity);
    assertThat(savedOrder.getPricePerUnit()).isEqualByComparingTo(price);
    assertThat(savedOrder.getFees()).isEqualByComparingTo(fees);
    assertThat(savedOrder.getCurrency()).isEqualTo(currency);
    assertThat(savedOrder.getExecutedAt()).isEqualTo(executedAt);
    assertThat(savedOrder.getNotes()).isEqualTo(notes);
    assertThat(savedOrder.getCreatedAt()).isNotNull();
    assertThat(savedOrder.getUpdatedAt()).isNotNull();
  }

  private Broker setupBroker(final UUID ownerSid) {
    final Broker broker = BrokerTestFactory.createBrokerWithOwner(ownerSid, "DEGIRO", null);
    return brokerRepository.save(broker);
  }

  private Asset setupAsset() {
    final StockExchange exchange = stockExchangeService.fetchStockExchange(new FetchStockExchangeCommand(".AS"));
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    asset.setStockExchange(exchange);
    return assetRepository.save(asset);
  }

}
