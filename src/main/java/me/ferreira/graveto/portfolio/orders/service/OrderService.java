package me.ferreira.graveto.portfolio.orders.service;

import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.service.command.CreateOrderCommand;
import me.ferreira.graveto.portfolio.orders.service.command.UpdateOrderCommand;

public interface OrderService {

  Order createOrder(CreateOrderCommand command);

  Order updateOrder(UpdateOrderCommand command);

}
