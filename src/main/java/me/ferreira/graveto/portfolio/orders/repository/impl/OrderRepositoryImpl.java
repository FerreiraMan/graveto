package me.ferreira.graveto.portfolio.orders.repository.impl;

import java.util.Optional;
import java.util.UUID;
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
  public Order save(final Order order) {
    return repository.save(order);
  }

  @Override
  public Optional<Order> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

}
