package me.ferreira.graveto.portfolio.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import org.junit.jupiter.api.Test;

public class OrderTest {

  @Test
  void shouldCreateOrderWithGeneratedSid() {
    // Arrange
    final Broker broker = new Broker();
    broker.setSid(UUID.randomUUID());

    final Asset asset = new Asset();
    asset.setSid(UUID.randomUUID());

    final UUID userSid = UUID.randomUUID();
    final BigDecimal quantity = new BigDecimal("10");
    final BigDecimal price = new BigDecimal("72.50");
    final BigDecimal fees = new BigDecimal("2.00");
    final LocalDateTime executedAt = LocalDateTime.now().minusDays(1);

    // Act
    final Order order = Order.create(broker, asset, userSid, OrderType.BUY, quantity, price, fees, Currency.EUR,
        executedAt, "first buy");

    // Assert
    assertThat(order.getSid()).isNotNull();
    assertThat(order.getBroker()).isEqualTo(broker);
    assertThat(order.getAsset()).isEqualTo(asset);
    assertThat(order.getUserSid()).isEqualTo(userSid);
    assertThat(order.getOrderType()).isEqualTo(OrderType.BUY);
    assertThat(order.getQuantity()).isEqualByComparingTo(quantity);
    assertThat(order.getPricePerUnit()).isEqualByComparingTo(price);
    assertThat(order.getFees()).isEqualByComparingTo(fees);
    assertThat(order.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(order.getExecutedAt()).isEqualTo(executedAt);
    assertThat(order.getNotes()).isEqualTo("first buy");
  }

  @Test
  void shouldDefaultFeesToZeroWhenNullIsProvided() {
    // Act
    final Order order = Order.create(new Broker(), new Asset(), UUID.randomUUID(), OrderType.BUY,
        BigDecimal.TEN, BigDecimal.TEN, null, Currency.EUR, LocalDateTime.now(), null);

    // Assert
    assertThat(order.getFees()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void shouldCreateOrderWithNullNotes() {
    // Act
    final Order order = Order.create(new Broker(), new Asset(), UUID.randomUUID(), OrderType.SELL,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, Currency.EUR, LocalDateTime.now(), null);

    // Assert
    assertThat(order.getNotes()).isNull();
  }

}
