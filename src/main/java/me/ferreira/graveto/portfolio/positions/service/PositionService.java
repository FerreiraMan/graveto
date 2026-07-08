package me.ferreira.graveto.portfolio.positions.service;

import java.math.BigDecimal;
import java.util.List;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPositionOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.payload.PositionValuation;

public interface PositionService {

  Position applyOrderToPosition(Order order);

  Position reapplyOrderToPosition(BigDecimal oldQuantity, BigDecimal oldPrice, BigDecimal oldFee, Order updatedOrder);

  List<PositionValuation> generatePositionValuationOverview(FetchPositionOverviewCommand command);

}
