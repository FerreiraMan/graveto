package me.ferreira.graveto.portfolio.orders;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
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
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
import me.ferreira.graveto.portfolio.orders.web.dto.request.UpdateOrderRequestDto;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import me.ferreira.graveto.portfolio.utils.BrokerTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateOrderIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private BrokerRepository brokerRepository;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private PositionRepository positionRepository;
  @Autowired
  private StockExchangeService stockExchangeService;

  @Test
  void shouldUpdateOrderAndRecalculatePosition() {
    // Scenario: Create a BUY order (10 @ 72.50, fee 2). Position: qty=10, avg=72.50, invested=727.
    // Action: Update order to (15 @ 70, fee 1).
    // Expected: Order fields updated, position recalculated to qty=15, avg=70, invested=1051.
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    // Create initial order
    final String orderSid = createOrder(userSid, broker, asset,
        OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"), "original");

    // Act — update the order
    final UpdateOrderRequestDto updateRequest = new UpdateOrderRequestDto(
        UUID.fromString(orderSid), new BigDecimal("15"), new BigDecimal("70"), new BigDecimal("1"), null, "updated");

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(updateRequest)
        .when()
        .patch("/orders")
        .then()
        .statusCode(200)
        .body("sid", equalTo(orderSid))
        .body("orderType", equalTo("BUY"))
        .body("notes", equalTo("updated"));

    // Assert — order updated in DB
    final var savedOrder = orderRepository.findBySid(UUID.fromString(orderSid)).orElseThrow();
    assertThat(savedOrder.getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
    assertThat(savedOrder.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("70"));
    assertThat(savedOrder.getFees()).isEqualByComparingTo(new BigDecimal("1"));
    assertThat(savedOrder.getNotes()).isEqualTo("updated");

    // Assert — position recalculated
    final Optional<Position> position = positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());
    assertThat(position).isPresent();
    assertThat(position.get().getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
    assertThat(position.get().getAverageCost()).isEqualByComparingTo(new BigDecimal("70"));
    assertThat(position.get().getTotalInvested()).isEqualByComparingTo(new BigDecimal("1051"));
  }

  @Test
  void shouldUpdateSellOrderAndRecalculatePositionQuantity() {
    // Scenario: BUY 10 @ 72.50 (fee 2) → position qty=10. Then SELL 3.
    //   Position: qty=7, avg=72.50, invested=727.
    // Action: Update SELL from 3 → 5.
    // Expected: Position qty=5, avgCost unchanged, totalInvested unchanged.
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    // Create BUY order
    createOrder(userSid, broker, asset,
        OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"), null);

    // Create SELL order
    final String sellOrderSid = createOrder(userSid, broker, asset,
        OrderType.SELL, new BigDecimal("3"), new BigDecimal("90"), BigDecimal.ZERO, null);

    // Act — update SELL quantity from 3 to 5
    final UpdateOrderRequestDto updateRequest = new UpdateOrderRequestDto(
        UUID.fromString(sellOrderSid), new BigDecimal("5"), null, null, null, null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(updateRequest)
        .when()
        .patch("/orders")
        .then()
        .statusCode(200);

    // Assert — position recalculated
    final Optional<Position> position = positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());
    assertThat(position).isPresent();
    assertThat(position.get().getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
    assertThat(position.get().getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(position.get().getTotalInvested()).isEqualByComparingTo(new BigDecimal("727"));
  }

  @Test
  void shouldKeepUnchangedFieldsWhenPartialUpdate() {
    // Scenario: BUY 10 @ 72.50, fee 2, notes "original".
    // Action: Update only quantity to 20 (other fields null).
    // Expected: price, fees, notes stay at original values.
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    final String orderSid = createOrder(userSid, broker, asset,
        OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"), "original");

    // Act — partial update: only quantity
    final UpdateOrderRequestDto updateRequest = new UpdateOrderRequestDto(
        UUID.fromString(orderSid), new BigDecimal("20"), null, null, null, null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(updateRequest)
        .when()
        .patch("/orders")
        .then()
        .statusCode(200);

    // Assert
    final var savedOrder = orderRepository.findBySid(UUID.fromString(orderSid)).orElseThrow();
    assertThat(savedOrder.getQuantity()).isEqualByComparingTo(new BigDecimal("20"));
    assertThat(savedOrder.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(savedOrder.getFees()).isEqualByComparingTo(new BigDecimal("2"));
    assertThat(savedOrder.getNotes()).isEqualTo("original");
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotExist() {
    // Act & Assert
    final UpdateOrderRequestDto request = new UpdateOrderRequestDto(
        UUID.randomUUID(), new BigDecimal("5"), null, null, null, null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + UUID.randomUUID())
        .body(request)
        .when()
        .patch("/orders")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturnForbiddenWhenUserLacksPermission() {
    // Arrange — create order as owner, try to update as different user
    final UUID ownerSid = UUID.randomUUID();
    final UUID otherUserSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(ownerSid, new UserResponseDto(ownerSid, "owner@example.com")));

    final Broker broker = setupBroker(ownerSid);
    final Asset asset = setupAsset();

    final String orderSid = createOrder(ownerSid, broker, asset,
        OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"), BigDecimal.ZERO, null);

    // Act — update as different user
    final UpdateOrderRequestDto request = new UpdateOrderRequestDto(
        UUID.fromString(orderSid), new BigDecimal("5"), null, null, null, null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + otherUserSid)
        .body(request)
        .when()
        .patch("/orders")
        .then()
        .statusCode(404);
  }

  private String createOrder(final UUID userSid, final Broker broker, final Asset asset,
                             final OrderType orderType, final BigDecimal quantity, final BigDecimal price,
                             final BigDecimal fees, final String notes) {

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(), asset.getSid(), orderType, quantity, price, fees,
        Currency.EUR, LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), notes);

    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(201)
        .extract()
        .path("sid");
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
