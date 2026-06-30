package me.ferreira.graveto.portfolio.orders.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.Order_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = {Order_.ASSET, Order_.BROKER})
  Optional<Order> findBySid(final UUID sid);

}
