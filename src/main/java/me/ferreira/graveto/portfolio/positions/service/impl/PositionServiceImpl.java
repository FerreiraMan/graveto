package me.ferreira.graveto.portfolio.positions.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class PositionServiceImpl implements PositionService {

  private final PositionRepository positionRepository;

  @Override
  @Transactional
  public Position applyOrderToPosition(final Order order) {

    final OrderType orderType = order.getOrderType();
    final BigDecimal orderQuantity = order.getQuantity();
    final BigDecimal pricePerUnit = order.getPricePerUnit();
    final BigDecimal fees = order.getFees();
    final Broker broker = order.getBroker();
    final Asset asset = order.getAsset();

    final Optional<Position> existingPosition =
        positionRepository.findByBrokerSidAndAssetSid(broker.getSid(), asset.getSid());

    if (existingPosition.isEmpty()) {
      final Position newPosition = Position.create(orderType, broker, asset, orderQuantity, pricePerUnit, fees);
      return positionRepository.save(newPosition);
    }

    final Position position = existingPosition.get();
    position.recalculateAverageCost(orderType, orderQuantity, pricePerUnit);
    position.updateQuantity(orderType, orderQuantity);
    position.updateTotalInvested(orderType, orderQuantity, pricePerUnit, fees);
    return positionRepository.save(position);
  }

  @Override
  @Transactional
  public Position reapplyOrderToPosition(final BigDecimal oldQuantity,
                                         final BigDecimal oldPrice, final BigDecimal oldFee, final Order updatedOrder) {

    final UUID brokerSid = updatedOrder.getBroker().getSid();
    final UUID assetSid = updatedOrder.getAsset().getSid();

    final Position existingPosition =
        positionRepository.findByBrokerSidAndAssetSid(brokerSid, assetSid)
            .orElseThrow(() -> new IllegalStateException(String.format(
                "Expected position from original order creation not found for broker [%s] and asset [%s].",
                brokerSid.toString(), assetSid.toString())));

    return processUpdatedOrder(existingPosition, oldQuantity, oldPrice, oldFee, updatedOrder);
  }

  private Position processUpdatedOrder(final Position existingPosition, final BigDecimal oldQuantity,
                                       final BigDecimal oldPrice, final BigDecimal oldFee,
                                       final Order updatedOrder) {

    final OrderType orderType = updatedOrder.getOrderType();

    existingPosition.reverseOrderImpact(orderType, oldQuantity, oldPrice, oldFee);
    existingPosition.recalculateAverageCost(orderType, updatedOrder.getQuantity(), updatedOrder.getPricePerUnit());
    existingPosition.updateQuantity(orderType, updatedOrder.getQuantity());
    existingPosition.updateTotalInvested(
        orderType, updatedOrder.getQuantity(), updatedOrder.getPricePerUnit(), updatedOrder.getFees());

    return positionRepository.save(existingPosition);
  }

}
