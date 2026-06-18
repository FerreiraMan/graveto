package me.ferreira.graveto.portfolio.orders.repository;

import me.ferreira.graveto.portfolio.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

}
