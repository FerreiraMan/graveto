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
    // Arrange
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
    // Arrange
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

  private BigDecimal calculateAverage(final BigDecimal originalAverage,
                                      final BigDecimal originalQuantity, final BigDecimal quantityFromNewOrder,
                                      final BigDecimal pricePerUnit) {

    final BigDecimal oldSum = originalAverage.multiply(originalQuantity);
    final BigDecimal newSum = oldSum.add(quantityFromNewOrder.multiply(pricePerUnit));
    final BigDecimal newCount = originalQuantity.add(quantityFromNewOrder);
    return newSum.divide(newCount, 8, RoundingMode.HALF_UP);
  }

}
