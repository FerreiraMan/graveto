package me.ferreira.graveto.portfolio.positions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import org.junit.jupiter.api.Test;

public class PositionTest {

  @Test
  void shouldCreatePositionWithGeneratedSid() {
    // Arrange
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final BigDecimal expectedTotalInvested = quantity.multiply(pricePerUnit).add(fees);

    // Act
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    // Assert
    assertThat(position.getSid()).isNotNull();
    assertThat(position.getBroker()).isEqualTo(broker);
    assertThat(position.getAsset()).isEqualTo(asset);
    assertThat(position.getQuantity()).isEqualTo(quantity);
    assertThat(position.getAverageCost()).isEqualTo(pricePerUnit);
    assertThat(position.getTotalInvested()).isEqualTo(expectedTotalInvested);
  }

  @Test
  void shouldThrowIfFirstPositionIsFromSellOrder() {

    // Act & Assert
    assertThatThrownBy(
        () -> Position.create(OrderType.SELL, mock(Broker.class), mock(Asset.class), BigDecimal.TEN, BigDecimal.ONE,
            BigDecimal.ZERO))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRecalculateAverageCostOnBuyOrder() {
    // Scenario: Position has 10 units @ 15.
    // New BUY order: 5 units @ 20.
    // Expected new average: (10×15 + 5×20) / (10+5) = 250/15 = 16.66666667
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);
    final BigDecimal newOrderPricePerUnit = BigDecimal.valueOf(20);
    final BigDecimal expectedNewAverage =
        calculateAverage(pricePerUnit, quantity, newOrderQuantity, newOrderPricePerUnit);

    // Act
    position.recalculateAverageCost(OrderType.BUY, newOrderQuantity, newOrderPricePerUnit);

    // Assert
    assertThat(position.getAverageCost()).isNotEqualByComparingTo(pricePerUnit);
    assertThat(position.getAverageCost()).isEqualByComparingTo(expectedNewAverage);
    assertThat(position.getAverageCost()).isEqualByComparingTo(new BigDecimal("16.66666667"));
  }

  @Test
  void shouldNotRecalculateAverageCostOnSellOrder() {
    // Arrange
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);
    final BigDecimal newOrderPricePerUnit = BigDecimal.valueOf(20);

    // Act
    position.recalculateAverageCost(OrderType.SELL, newOrderQuantity, newOrderPricePerUnit);

    // Assert
    assertThat(position.getAverageCost()).isEqualByComparingTo(pricePerUnit);
  }

  @Test
  void shouldIncreaseQuantityOnBuyOrder() {
    // Arrange
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);

    // Act
    position.updateQuantity(OrderType.BUY, newOrderQuantity);

    // Assert
    assertThat(position.getQuantity()).isEqualByComparingTo(quantity.add(newOrderQuantity));
  }

  @Test
  void shouldDecreaseQuantityOnSellOrder() {
    // Arrange
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);

    // Act
    position.updateQuantity(OrderType.SELL, newOrderQuantity);

    // Assert
    assertThat(position.getQuantity()).isEqualByComparingTo(quantity.subtract(newOrderQuantity));
  }

  @Test
  void shouldAccumulateTotalInvestedOnBuyOrder() {
    // Scenario: Position created from BUY 10 @ 15 + 1 fee = 151 total invested.
    // New BUY order: 5 @ 20 + 2 fee = 102 added.
    // Expected total invested: 151 + 102 = 253
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);
    final BigDecimal newOrderPricePerUnit = BigDecimal.valueOf(20);
    final BigDecimal newOrderFees = BigDecimal.valueOf(2);
    final BigDecimal newTotalInvested =
        position.getTotalInvested().add(newOrderQuantity.multiply(newOrderPricePerUnit).add(newOrderFees));

    // Act
    position.updateTotalInvested(OrderType.BUY, newOrderQuantity, newOrderPricePerUnit, newOrderFees);

    // Assert
    assertThat(position.getTotalInvested()).isEqualByComparingTo(newTotalInvested);
    assertThat(position.getTotalInvested()).isEqualByComparingTo(new BigDecimal("253"));
  }

  @Test
  void shouldNotChangeTotalInvestedOnSellOrder() {
    // Arrange
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final BigDecimal quantity = BigDecimal.valueOf(10);
    final BigDecimal pricePerUnit = BigDecimal.valueOf(15);
    final BigDecimal fees = BigDecimal.valueOf(1);
    final Position position = Position.create(OrderType.BUY, broker, asset, quantity, pricePerUnit, fees);
    final BigDecimal expectedTotalInvested = quantity.multiply(pricePerUnit).add(fees);

    final BigDecimal newOrderQuantity = BigDecimal.valueOf(5);
    final BigDecimal newOrderPricePerUnit = BigDecimal.valueOf(20);
    final BigDecimal newOrderFees = BigDecimal.valueOf(2);

    // Act
    position.updateTotalInvested(OrderType.SELL, newOrderQuantity, newOrderPricePerUnit, newOrderFees);

    // Assert
    assertThat(position.getTotalInvested()).isEqualByComparingTo(expectedTotalInvested);
  }

  @Test
  void shouldReverseFullBuyOrderImpactOnPosition() {
    // Scenario: Position built from two BUY orders:
    //   Order 1: 10 units @ 60 + 1 fee = 601 invested
    //   Order 2: 10 units @ 80 + 2 fee = 802 invested
    //   Position state: qty=20, avgCost=70, totalInvested=1403
    //
    // Action: Reverse order 2 (10@80, fee 2)
    // Expected: Position returns to order 1 state: qty=10, avgCost=60, totalInvested=601
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final Position position = Position.create(OrderType.BUY, broker, asset,
        BigDecimal.valueOf(10), BigDecimal.valueOf(60), BigDecimal.valueOf(1));
    position.recalculateAverageCost(OrderType.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(80));
    position.updateQuantity(OrderType.BUY, BigDecimal.valueOf(10));
    position.updateTotalInvested(OrderType.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(80), BigDecimal.valueOf(2));

    // Act
    position.reverseOrderImpact(OrderType.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(80), BigDecimal.valueOf(2));

    // Assert
    assertThat(position.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(position.getAverageCost()).isEqualByComparingTo(new BigDecimal("60"));
    assertThat(position.getTotalInvested()).isEqualByComparingTo(new BigDecimal("601"));
  }

  @Test
  void shouldReverseSellOrderImpactOnPosition() {
    // Scenario: Position has 10 units @ 72.50 (fee 2, totalInvested=727).
    //   Then SELL 3 units → qty drops to 7. AvgCost/totalInvested unchanged (SELL doesn't affect them).
    //
    // Action: Reverse the SELL of 3 units.
    // Expected: qty restored to 10, avgCost=72.50, totalInvested=727
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final Position position = Position.create(OrderType.BUY, broker, asset,
        BigDecimal.valueOf(10), BigDecimal.valueOf(72.50), BigDecimal.valueOf(2));
    position.updateQuantity(OrderType.SELL, BigDecimal.valueOf(3));

    // Act
    position.reverseOrderImpact(OrderType.SELL, BigDecimal.valueOf(3), BigDecimal.valueOf(90), BigDecimal.ZERO);

    // Assert
    assertThat(position.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(position.getAverageCost()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(position.getTotalInvested()).isEqualByComparingTo(new BigDecimal("727"));
  }

  @Test
  void shouldSetAverageCostToZeroWhenReversingOnlyBuyOrder() {
    // Scenario: Position has only 1 BUY order: 10 @ 72.50, fee 2 (totalInvested=727).
    //
    // Action: Reverse that single order (effectively emptying the position).
    // Expected: qty=0, avgCost=0, totalInvested=0 (division by zero guard kicks in)
    final Broker broker = Broker.create("Degiro", UUID.randomUUID(), Currency.EUR);
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final Position position = Position.create(OrderType.BUY, broker, asset,
        BigDecimal.valueOf(10), BigDecimal.valueOf(72.50), BigDecimal.valueOf(2));

    // Act
    position.reverseOrderImpact(OrderType.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(72.50),
        BigDecimal.valueOf(2));

    // Assert
    assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(position.getAverageCost()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(position.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  private BigDecimal calculateAverage(final BigDecimal originalAverage,
                                      final BigDecimal originalQuantity, final BigDecimal quantityFromNewOrder,
                                      final BigDecimal pricePerUnit) {

    final BigDecimal oldSum = originalAverage.multiply(originalQuantity);
    final BigDecimal newSum = oldSum.add(quantityFromNewOrder.multiply(pricePerUnit));
    final BigDecimal newCount = originalQuantity.add(quantityFromNewOrder);
    return newSum.divide(newCount, 8, RoundingMode.HALF_UP);
  }

}
