package me.ferreira.graveto.portfolio.positions.service;

import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.positions.domain.Position;

public interface PositionService {

  Position applyOrderToPosition(Order order);

}
