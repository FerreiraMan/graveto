package me.ferreira.graveto.portfolio.positions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import me.ferreira.graveto.portfolio.positions.service.impl.PositionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplyOrderToPositionServiceImplTest {

  @InjectMocks
  private PositionServiceImpl positionService;
  @Mock
  private PositionRepository positionRepository;

  @Test
  void shouldCreateNewPositionWhenNoneExistsForBuyOrder() {
    // Arrange
    final Order order =
        buildOrder(OrderType.BUY, new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"));

    when(positionRepository.findByBrokerSidAndAssetSid(order.getBroker().getSid(), order.getAsset().getSid()))
        .thenReturn(Optional.empty());
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.applyOrderToPosition(order);

    // Assert
    final ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
    verify(positionRepository).save(captor.capture());

    final Position saved = captor.getValue();
    assertThat(saved.getSid()).isNotNull();
    assertThat(saved.getBroker()).isEqualTo(order.getBroker());
    assertThat(saved.getAsset()).isEqualTo(order.getAsset());
    assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(saved.getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(saved.getTotalInvested()).isEqualByComparingTo(new BigDecimal("727.00"));
    assertThat(result).isEqualTo(saved);
  }

  @Test
  void shouldThrowWhenFirstOrderIsSellAndNoPositionExists() {
    // Arrange
    final Order order = buildOrder(OrderType.SELL, new BigDecimal("5"), new BigDecimal("80.00"), BigDecimal.ZERO);

    when(positionRepository.findByBrokerSidAndAssetSid(order.getBroker().getSid(), order.getAsset().getSid()))
        .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> positionService.applyOrderToPosition(order))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create a position from a SELL order — position must exist before selling.");
  }

  @Test
  void shouldUpdateExistingPositionOnBuyOrder() {
    // Arrange
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();
    final Order order =
        buildOrder(broker, asset, OrderType.BUY, new BigDecimal("5"), new BigDecimal("80.00"), new BigDecimal("1.50"));

    final Position existingPosition = Position.create(OrderType.BUY, broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"));

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.of(existingPosition));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.applyOrderToPosition(order);

    // Assert
    verify(positionRepository).save(existingPosition);
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
    assertThat(result.getAverageCost()).isNotEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("1128.50"));
  }

  @Test
  void shouldUpdateExistingPositionOnSellOrder() {
    // Arrange
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();
    final Order order =
        buildOrder(broker, asset, OrderType.SELL, new BigDecimal("3"), new BigDecimal("90.00"), BigDecimal.ZERO);

    final Position existingPosition = Position.create(OrderType.BUY, broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"));

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.of(existingPosition));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.applyOrderToPosition(order);

    // Assert
    verify(positionRepository).save(existingPosition);
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("7"));
    assertThat(result.getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("727.00"));
  }

  private static Order buildOrder(final OrderType orderType, final BigDecimal quantity,
                                  final BigDecimal price, final BigDecimal fees) {
    return buildOrder(buildBroker(), buildAsset(), orderType, quantity, price, fees);
  }

  private static Order buildOrder(final Broker broker, final Asset asset, final OrderType orderType,
                                  final BigDecimal quantity, final BigDecimal price, final BigDecimal fees) {
    return Order.create(broker, asset, UUID.randomUUID(), orderType, quantity, price, fees,
        Currency.EUR, LocalDateTime.now(), null);
  }

  private static Broker buildBroker() {
    final Broker broker = new Broker();
    broker.setSid(UUID.randomUUID());
    broker.setName("DEGIRO");
    broker.setCurrency(Currency.EUR);
    return broker;
  }

  private static Asset buildAsset() {
    final Asset asset = new Asset();
    asset.setSid(UUID.randomUUID());
    asset.setTicker("IWDA");
    asset.setName("iShares Core MSCI World");
    asset.setAssetType(AssetType.ETF);
    asset.setCurrency(Currency.EUR);
    return asset;
  }

}
