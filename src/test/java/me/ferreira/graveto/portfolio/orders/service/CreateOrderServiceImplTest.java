package me.ferreira.graveto.portfolio.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.AssetNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InsufficientPermissionsOnBrokerException;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerStatus;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import me.ferreira.graveto.portfolio.orders.service.command.CreateOrderCommand;
import me.ferreira.graveto.portfolio.orders.service.impl.OrderServiceImpl;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateOrderServiceImplTest {

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
  void shouldThrowIfBrokerIsNotFoundDuringOrderCreation() {
    // Arrange
    final UUID brokerSid = UUID.randomUUID();
    when(brokerService.fetchBrokerEntity(brokerSid)).thenThrow(new BrokerNotFoundException(brokerSid));

    // Act & Assert
    assertThatThrownBy(() -> orderService.createOrder(buildCommand(brokerSid, UUID.randomUUID(), UUID.randomUUID())))
        .isInstanceOf(BrokerNotFoundException.class)
        .hasMessage("Broker with SID [" + brokerSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldThrowIfUserIsNotAuthorizedToCreateOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = buildBroker(UUID.randomUUID(), UUID.randomUUID(), BrokerMembershipRole.VIEWER);

    when(brokerService.fetchBrokerEntity(broker.getSid())).thenReturn(broker);

    // Act & Assert
    assertThatThrownBy(() -> orderService.createOrder(buildCommand(broker.getSid(), UUID.randomUUID(), userSid)))
        .isInstanceOf(InsufficientPermissionsOnBrokerException.class)
        .hasMessage("User does not have the required role to create orders for this broker account.");
  }

  @Test
  void shouldThrowIfAssetIsNotFoundDuringOrderCreation() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = buildBroker(UUID.randomUUID(), userSid, BrokerMembershipRole.OWNER);
    final UUID assetSid = UUID.randomUUID();

    when(brokerService.fetchBrokerEntity(broker.getSid())).thenReturn(broker);
    when(assetService.fetchAsset(any())).thenThrow(new AssetNotFoundException(assetSid));

    // Act & Assert
    assertThatThrownBy(() -> orderService.createOrder(buildCommand(broker.getSid(), assetSid, userSid)))
        .isInstanceOf(AssetNotFoundException.class)
        .hasMessage("Asset with SID [" + assetSid + "] was not found or you do not have permission to view it.");
  }

  @Test
  void shouldCreateOrder() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = buildBroker(UUID.randomUUID(), userSid, BrokerMembershipRole.OWNER);
    final Asset asset = buildAsset(UUID.randomUUID());
    final CreateOrderCommand command = new CreateOrderCommand(
        userSid, broker.getSid(), asset.getSid(), OrderType.BUY,
        new BigDecimal("10"), new BigDecimal("72.50"), new BigDecimal("2.00"),
        Currency.EUR, LocalDateTime.now().minusDays(1), "first buy"
    );

    when(brokerService.fetchBrokerEntity(broker.getSid())).thenReturn(broker);
    when(assetService.fetchAsset(any(FetchAssetCommand.class))).thenReturn(asset);
    when(orderRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Order result = orderService.createOrder(command);

    // Assert
    final ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());

    final Order savedOrder = orderCaptor.getValue();
    assertThat(savedOrder.getSid()).isNotNull();
    assertThat(savedOrder.getBroker()).isEqualTo(broker);
    assertThat(savedOrder.getAsset()).isEqualTo(asset);
    assertThat(savedOrder.getUserSid()).isEqualTo(userSid);
    assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.BUY);
    assertThat(savedOrder.getQuantity()).isEqualByComparingTo(command.quantity());
    assertThat(savedOrder.getPricePerUnit()).isEqualByComparingTo(command.price());
    assertThat(savedOrder.getFees()).isEqualByComparingTo(command.fees());
    assertThat(savedOrder.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(savedOrder.getExecutedAt()).isEqualTo(command.executedAt());
    assertThat(savedOrder.getNotes()).isEqualTo("first buy");

    assertThat(result).isEqualTo(savedOrder);

    verify(positionService).applyOrderToPosition(savedOrder);
  }

  @Test
  void shouldDefaultFeesToZeroWhenNotProvidedOnOrderCreation() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Broker broker = buildBroker(UUID.randomUUID(), userSid, BrokerMembershipRole.OWNER);
    final Asset asset = buildAsset(UUID.randomUUID());
    final CreateOrderCommand command = new CreateOrderCommand(
        userSid, broker.getSid(), asset.getSid(), OrderType.BUY,
        new BigDecimal("10"), new BigDecimal("72.50"), null,
        Currency.EUR, LocalDateTime.now(), null
    );

    when(brokerService.fetchBrokerEntity(broker.getSid())).thenReturn(broker);
    when(assetService.fetchAsset(any())).thenReturn(asset);
    when(orderRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    orderService.createOrder(command);

    // Assert
    final ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(captor.capture());
    assertThat(captor.getValue().getFees()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  private static CreateOrderCommand buildCommand(final UUID brokerSid, final UUID assetSid, final UUID userSid) {
    return new CreateOrderCommand(
        userSid, brokerSid, assetSid, OrderType.BUY,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
        Currency.EUR, LocalDateTime.now(), null
    );
  }

  private static Broker buildBroker(final UUID sid, final UUID ownerUserSid, final BrokerMembershipRole role) {
    final Broker broker = new Broker();
    broker.setSid(sid);
    broker.setName("DEGIRO");
    broker.setCurrency(Currency.EUR);
    broker.setStatus(BrokerStatus.ACTIVE);

    final BrokerMembership membership = new BrokerMembership();
    membership.setUserSid(ownerUserSid);
    membership.setRole(role);
    membership.setBroker(broker);
    broker.getMemberships().add(membership);

    return broker;
  }

  private static Asset buildAsset(final UUID sid) {
    final Asset asset = new Asset();
    asset.setSid(sid);
    asset.setTicker("IWDA");
    asset.setName("iShares Core MSCI World ETF");
    asset.setCurrency(Currency.EUR);
    return asset;
  }

}
