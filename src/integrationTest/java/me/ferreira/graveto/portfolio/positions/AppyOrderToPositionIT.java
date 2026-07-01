package me.ferreira.graveto.portfolio.positions;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import me.ferreira.graveto.portfolio.orders.web.dto.request.CreateOrderRequestDto;
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
public class AppyOrderToPositionIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private PositionRepository positionRepository;
  @Autowired
  private BrokerRepository brokerRepository;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private StockExchangeService stockExchangeService;

  @Test
  void shouldCreatePositionOnFirstBuyOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    final CreateOrderRequestDto request = new CreateOrderRequestDto(
        broker.getSid(), asset.getSid(), OrderType.BUY,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"),
        Currency.EUR, LocalDateTime.now().minusDays(1), "first buy"
    );

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(request)
        .when()
        .post("/orders")
        .then()
        .statusCode(201)
        .body("sid", notNullValue());

    // Assert
    final Optional<Position> position = positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());
    assertThat(position).isPresent();
    assertThat(position.get().getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(position.get().getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(position.get().getTotalInvested()).isEqualByComparingTo(new BigDecimal("727.00"));
  }

  @Test
  void shouldUpdatePositionOnSecondBuyOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new CreateOrderRequestDto(
            broker.getSid(), asset.getSid(), OrderType.BUY,
            new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"),
            Currency.EUR, LocalDateTime.now().minusDays(2), "first buy"))
        .when()
        .post("/orders")
        .then()
        .statusCode(201);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new CreateOrderRequestDto(
            broker.getSid(), asset.getSid(), OrderType.BUY,
            new BigDecimal("5"), new BigDecimal("80.00"), new BigDecimal("1.50"),
            Currency.EUR, LocalDateTime.now().minusDays(1), "second buy"))
        .when()
        .post("/orders")
        .then()
        .statusCode(201);

    // Assert
    final Optional<Position> position = positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());
    assertThat(position).isPresent();
    assertThat(position.get().getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
    assertThat(position.get().getTotalInvested()).isEqualByComparingTo(new BigDecimal("1128.50"));
  }

  @Test
  void shouldDecreaseQuantityOnSellOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(userApi.fetchUserDetailsByUserSids(any()))
        .thenReturn(Map.of(userSid, new UserResponseDto(userSid, "user@example.com")));

    final Broker broker = setupBroker(userSid);
    final Asset asset = setupAsset();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new CreateOrderRequestDto(
            broker.getSid(), asset.getSid(), OrderType.BUY,
            new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"),
            Currency.EUR, LocalDateTime.now().minusDays(2), null))
        .when()
        .post("/orders")
        .then()
        .statusCode(201);

    // Act
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + userSid)
        .body(new CreateOrderRequestDto(
            broker.getSid(), asset.getSid(), OrderType.SELL,
            new BigDecimal("3"), new BigDecimal("90.00"), BigDecimal.ZERO,
            Currency.EUR, LocalDateTime.now().minusDays(1), null))
        .when()
        .post("/orders")
        .then()
        .statusCode(201);

    // Assert
    final Optional<Position> position = positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());
    assertThat(position).isPresent();
    assertThat(position.get().getQuantity()).isEqualByComparingTo(new BigDecimal("7"));
    assertThat(position.get().getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(position.get().getTotalInvested()).isEqualByComparingTo(new BigDecimal("727.00"));
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
