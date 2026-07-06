package me.ferreira.graveto.portfolio.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.common.web.exception.portfolio.OrderNotFoundException;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import me.ferreira.graveto.portfolio.orders.service.command.UpdateOrderCommand;
import me.ferreira.graveto.portfolio.orders.service.impl.OrderServiceImpl;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateOrderServiceImplTest {

  @InjectMocks
  private OrderServiceImpl orderService;
  @Mock
  private BrokerService brokerService;
  @Mock
  private AssetService assetService;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private PositionService positionService;

  @Test
  void shouldThrowWhenOrderIsNotFound() {
    // Arrange
    final UUID orderSid = UUID.randomUUID();
    final UUID userSid = UUID.randomUUID();
    final UpdateOrderCommand command = buildCommand(userSid, orderSid, new BigDecimal("5"), null, null, null, null);

    when(orderRepository.findBySidAndUserSid(orderSid, userSid)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> orderService.updateOrder(command))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldThrowWhenUserLacksPermission() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final Order existingOrder = buildExistingOrder(orderSid, UUID.randomUUID());
    final UpdateOrderCommand command = buildCommand(userSid, orderSid, new BigDecimal("5"), null, null, null, null);

    when(orderRepository.findBySidAndUserSid(orderSid, userSid)).thenReturn(Optional.of(existingOrder));

    // Act & Assert
    assertThatThrownBy(() -> orderService.updateOrder(command))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class);
  }

  @Test
  void shouldUpdateOrderAndReapplyPosition() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final Order existingOrder = buildExistingOrder(orderSid, userSid);
    final BigDecimal newQuantity = new BigDecimal("15");
    final BigDecimal newPrice = new BigDecimal("80.00");
    final BigDecimal newFees = new BigDecimal("3.00");
    final LocalDateTime newExecutedAt = LocalDateTime.now().minusDays(5);
    final UpdateOrderCommand command =
        buildCommand(userSid, orderSid, newQuantity, newPrice, newFees, newExecutedAt, "updated");

    when(orderRepository.findBySidAndUserSid(orderSid, userSid)).thenReturn(Optional.of(existingOrder));

    // Act
    final Order result = orderService.updateOrder(command);

    // Assert
    assertThat(result.getQuantity()).isEqualByComparingTo(newQuantity);
    assertThat(result.getPricePerUnit()).isEqualByComparingTo(newPrice);
    assertThat(result.getFees()).isEqualByComparingTo(newFees);
    assertThat(result.getExecutedAt()).isEqualTo(newExecutedAt);
    assertThat(result.getNotes()).isEqualTo("updated");

    verify(positionService).reapplyOrderToPosition(
        eq(new BigDecimal("10")),
        eq(new BigDecimal("72.50")),
        eq(new BigDecimal("2.00")),
        eq(existingOrder));
  }

  @Test
  void shouldKeepOldValuesWhenFieldsAreNull() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final Order existingOrder = buildExistingOrder(orderSid, userSid);
    final UpdateOrderCommand command = buildCommand(userSid, orderSid, null, null, null, null, null);

    when(orderRepository.findBySidAndUserSid(orderSid, userSid)).thenReturn(Optional.of(existingOrder));

    // Act
    final Order result = orderService.updateOrder(command);

    // Assert
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(result.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(result.getFees()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getNotes()).isEqualTo("original note");

    verify(positionService).reapplyOrderToPosition(
        eq(new BigDecimal("10")),
        eq(new BigDecimal("72.50")),
        eq(new BigDecimal("2.00")),
        eq(existingOrder));
  }

  @Test
  void shouldPartiallyUpdateOnlyProvidedFields() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID orderSid = UUID.randomUUID();
    final Order existingOrder = buildExistingOrder(orderSid, userSid);
    final UpdateOrderCommand command = buildCommand(userSid, orderSid, new BigDecimal("20"), null, null, null, null);

    when(orderRepository.findBySidAndUserSid(orderSid, userSid)).thenReturn(Optional.of(existingOrder));

    // Act
    final Order result = orderService.updateOrder(command);

    // Assert
    assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("20"));
    assertThat(result.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(result.getFees()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getNotes()).isEqualTo("original note");
  }

  private static UpdateOrderCommand buildCommand(final UUID userSid, final UUID orderSid,
                                                 final BigDecimal quantity, final BigDecimal price,
                                                 final BigDecimal fees, final LocalDateTime executedAt,
                                                 final String notes) {
    return new UpdateOrderCommand(userSid, orderSid, quantity, price, fees, executedAt, notes);
  }

  private static Order buildExistingOrder(final UUID orderSid, final UUID ownerSid) {
    final Broker broker = new Broker();
    broker.setSid(UUID.randomUUID());
    broker.setName("DEGIRO");
    broker.setCurrency(Currency.EUR);
    broker.setStatus(BrokerStatus.ACTIVE);

    final BrokerMembership membership = new BrokerMembership();
    membership.setUserSid(ownerSid);
    membership.setRole(BrokerMembershipRole.OWNER);
    membership.setBroker(broker);
    broker.getMemberships().add(membership);

    final Asset asset = new Asset();
    asset.setSid(UUID.randomUUID());
    asset.setTicker("IWDA");
    asset.setAssetType(AssetType.ETF);
    asset.setCurrency(Currency.EUR);

    final Order order = new Order();
    order.setSid(orderSid);
    order.setBroker(broker);
    order.setAsset(asset);
    order.setUserSid(ownerSid);
    order.setOrderType(OrderType.BUY);
    order.setQuantity(new BigDecimal("10"));
    order.setPricePerUnit(new BigDecimal("72.50"));
    order.setFees(new BigDecimal("2.00"));
    order.setCurrency(Currency.EUR);
    order.setExecutedAt(LocalDateTime.now().minusDays(1));
    order.setNotes("original note");
    return order;
  }

}
