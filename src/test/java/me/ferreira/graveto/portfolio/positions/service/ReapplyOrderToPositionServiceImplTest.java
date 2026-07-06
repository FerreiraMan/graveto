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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReapplyOrderToPositionServiceImplTest {

  @InjectMocks
  private PositionServiceImpl positionService;
  @Mock
  private PositionRepository positionRepository;

  @Test
  void shouldThrowWhenPositionNotFound() {
    // Arrange
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();
    final Order updatedOrder =
        buildOrder(broker, asset, OrderType.BUY, new BigDecimal("5"), new BigDecimal("80"), BigDecimal.ZERO);

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> positionService.reapplyOrderToPosition(
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"), updatedOrder))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Expected position from original order creation not found");
  }

  @Test
  void shouldReverseOldBuyAndApplyNewBuy() {
    // Scenario: Position built from two BUY orders:
    //   Order 1: 10 units @ 72.50 + 2 fee = 727 invested
    //   Order 2: 5 units @ 80 + 1.50 fee = 401.50 invested
    //   Position state: qty=15, totalInvested=1128.50
    //
    // Action: Edit order 2 from (5@80, fee 1.50) → (7@75, fee 2)
    // Expected: Reverse order 2, apply new values → qty=17, totalInvested=727+(7×75+2)=1254
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();

    final Position position = Position.create(OrderType.BUY, broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"));
    position.recalculateAverageCost(OrderType.BUY, new BigDecimal("5"), new BigDecimal("80"));
    position.updateQuantity(OrderType.BUY, new BigDecimal("5"));
    position.updateTotalInvested(OrderType.BUY, new BigDecimal("5"), new BigDecimal("80"), new BigDecimal("1.50"));

    final Order updatedOrder =
        buildOrder(broker, asset, OrderType.BUY, new BigDecimal("7"), new BigDecimal("75"), new BigDecimal("2"));

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.of(position));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.reapplyOrderToPosition(
        new BigDecimal("5"), new BigDecimal("80"), new BigDecimal("1.50"), updatedOrder);

    // Assert
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("17"));
    assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("1254"));
    verify(positionRepository).save(position);
  }

  @Test
  void shouldReverseOldSellAndApplyNewSell() {
    // Scenario: Position from BUY 10 @ 72.50 (fee 2, totalInvested=727), then SELL 3.
    //   Position state: qty=7, avgCost=72.50, totalInvested=727
    //
    // Action: Edit the SELL from 3 → 5 units.
    // Expected: Reverse old SELL (qty back to 10), apply new SELL of 5 → qty=5.
    //   AvgCost and totalInvested unchanged (SELL doesn't affect them).
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();

    final Position position = Position.create(OrderType.BUY, broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"));
    position.updateQuantity(OrderType.SELL, new BigDecimal("3"));

    final Order updatedOrder =
        buildOrder(broker, asset, OrderType.SELL, new BigDecimal("5"), new BigDecimal("90"), BigDecimal.ZERO);

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.of(position));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.reapplyOrderToPosition(
        new BigDecimal("3"), new BigDecimal("90"), BigDecimal.ZERO, updatedOrder);

    // Assert
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
    assertThat(result.getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("727"));
    verify(positionRepository).save(position);
  }

  @Test
  void shouldHandleSingleOrderPositionUpdate() {
    // Scenario: Position has only 1 BUY order: 10 @ 72.50, fee 2 (totalInvested=727).
    //
    // Action: Edit that order to 15 @ 70, fee 1.
    // Expected: Reverse empties position (qty=0, avg=0, invested=0),
    //   then apply new → qty=15, avgCost=70, totalInvested=15×70+1=1051
    final Broker broker = buildBroker();
    final Asset asset = buildAsset();

    final Position position = Position.create(OrderType.BUY, broker, asset,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"));

    final Order updatedOrder =
        buildOrder(broker, asset, OrderType.BUY, new BigDecimal("15"), new BigDecimal("70"), new BigDecimal("1"));

    when(positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid()))
        .thenReturn(Optional.of(position));
    when(positionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Position result = positionService.reapplyOrderToPosition(
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2"), updatedOrder);

    // Assert — position should reflect only the new order
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("15"));
    assertThat(result.getAverageCost()).isEqualByComparingTo(new BigDecimal("70"));
    assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("1051"));
    verify(positionRepository).save(position);
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
    asset.setAssetType(AssetType.ETF);
    asset.setCurrency(Currency.EUR);
    return asset;
  }

}
