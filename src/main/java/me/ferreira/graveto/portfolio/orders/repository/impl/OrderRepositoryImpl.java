package me.ferreira.graveto.portfolio.orders.repository.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.repository.OrderJpaRepository;
import me.ferreira.graveto.portfolio.orders.repository.OrderRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

  private final OrderJpaRepository repository;

  @Override
  public Order save(Order order) {
    return repository.save(order);
  }

}
