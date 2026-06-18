package me.ferreira.graveto.portfolio.orders.repository;

import me.ferreira.graveto.portfolio.orders.domain.Order;

public interface OrderRepository {

  Order save(Order order);

}
