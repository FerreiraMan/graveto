package me.ferreira.graveto.portfolio.positions.service;

import java.math.BigDecimal;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.positions.domain.Position;

public interface PositionService {

  Position applyOrderToPosition(Order order);

  Position reapplyOrderToPosition(BigDecimal oldQuantity, BigDecimal oldPrice, BigDecimal oldFee, Order updatedOrder);

}
