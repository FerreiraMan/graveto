package me.ferreira.graveto.portfolio.orders.domain.event;

import me.ferreira.graveto.portfolio.orders.domain.Order;

public record OrderCreatedEvent(
    Order order
) {
}
