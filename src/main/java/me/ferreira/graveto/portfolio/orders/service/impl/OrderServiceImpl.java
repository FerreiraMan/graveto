package me.ferreira.graveto.portfolio.orders.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.event.OrderCreatedEvent;
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import me.ferreira.graveto.portfolio.orders.service.OrderService;
import me.ferreira.graveto.portfolio.orders.service.command.CreateOrderCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final BrokerService brokerService;
  private final AssetService assetService;
  private final OrderRepository orderRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public Order createOrder(final CreateOrderCommand command) {

    final Broker broker = brokerService.fetchBroker(new FetchBrokerCommand(command.brokerSid()));
    broker.validateUserPermission(command.userSid(), BrokerMembershipRole::canCreateOrders, "create orders");

    final Asset asset = assetService.fetchAsset(new FetchAssetCommand(command.assetSid()));

    final Order createdOrder =
        Order.create(broker, asset, command.userSid(), command.orderType(), command.quantity(), command.price(),
            command.fees(), command.currency(), command.executedAt(), command.notes());

    orderRepository.save(createdOrder);

    log.info("Order created successfully. OrderSid: {}", createdOrder.getSid());
    eventPublisher.publishEvent(new OrderCreatedEvent(createdOrder));

    return createdOrder;
  }

}
