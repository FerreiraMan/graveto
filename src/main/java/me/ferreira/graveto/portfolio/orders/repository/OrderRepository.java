package me.ferreira.graveto.portfolio.orders.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.orders.domain.Order;

public interface OrderRepository {

  Order save(Order order);

  Optional<Order> findBySid(UUID sid);

  Optional<Order> findBySidAndUserSid(UUID sid, UUID userSid);

}
