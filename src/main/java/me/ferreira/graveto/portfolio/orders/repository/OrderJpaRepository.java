package me.ferreira.graveto.portfolio.orders.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.Order_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = {Order_.ASSET, Order_.BROKER})
  Optional<Order> findBySid(final UUID sid);

  @Query("SELECT o FROM Order o JOIN FETCH o.broker b JOIN FETCH b.memberships JOIN FETCH o.asset " +
         "WHERE o.sid = ?1 AND o.userSid = ?2")
  Optional<Order> findBySidAndUserSid(final UUID sid, final UUID userSid);

}
