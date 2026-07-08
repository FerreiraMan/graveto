package me.ferreira.graveto.portfolio.positions.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.orders.domain.Order;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import me.ferreira.graveto.portfolio.positions.service.PositionService;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPortfolioOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.command.FetchPositionOverviewCommand;
import me.ferreira.graveto.portfolio.positions.service.payload.PortfolioSummary;
import me.ferreira.graveto.portfolio.positions.service.payload.PositionValuation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class PositionServiceImpl implements PositionService {

  private final PositionRepository positionRepository;
  private final BrokerService brokerService;

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

  @Override
  @Transactional(readOnly = true)
  public List<PositionValuation> generatePositionValuationOverview(final FetchPositionOverviewCommand command) {

    brokerService
        .fetchBrokerEntity(command.brokerSid())
        .validateUserPermission(command.userSid(), BrokerMembershipRole::canRequestValuationOverview,
            "request position valuation overview");

    final List<Position> positionAndAssetOverview =
        positionRepository.fetchAllByBrokerSidWithAsset(command.brokerSid());

    return buildPositionValuation(positionAndAssetOverview);
  }

  @Override
  @Transactional(readOnly = true)
  public PortfolioSummary generatePortfolioValuationOverview(final FetchPortfolioOverviewCommand command) {

    brokerService
        .fetchBrokerEntity(command.brokerSid())
        .validateUserPermission(command.userSid(), BrokerMembershipRole::canRequestPortfolioOverview,
            "request portfolio valuation overview");

    final List<Position> positionAndAssetOverview =
        positionRepository.fetchAllByBrokerSidWithAsset(command.brokerSid());

    final List<PositionValuation> positionValuationList = buildPositionValuation(positionAndAssetOverview);

    return buildPortfolioSummary(positionValuationList);
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

  private List<PositionValuation> buildPositionValuation(final List<Position> positionList) {

    final List<PositionValuation> results = new ArrayList<>();

    positionList.forEach(position -> {

      final Asset asset = position.getAsset();

      if (asset.getCurrentPrice() == null) {
        log.warn("Asset [{}] has no current price. Skipping valuation.", asset.getTicker());
        return;
      }

      final BigDecimal unrealizedPnL =
          position.getQuantity().multiply(asset.getCurrentPrice()).subtract(position.getTotalInvested());
      final BigDecimal unrealizedPnlPercent =
          unrealizedPnL.divide(position.getTotalInvested(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

      results.add(new PositionValuation(
          asset.getSid(),
          asset.getTicker(),
          position.getQuantity(),
          position.getAverageCost(),
          position.getTotalInvested(),
          asset.getCurrentPrice(),
          position.getQuantity().multiply(asset.getCurrentPrice()),
          unrealizedPnL,
          unrealizedPnlPercent
      ));
    });

    return results;
  }

  private PortfolioSummary buildPortfolioSummary(final List<PositionValuation> positionValuationList) {

    if (positionValuationList.isEmpty()) {
      return new PortfolioSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    final BigDecimal totalInvested =
        positionValuationList.stream().map(PositionValuation::totalInvested).reduce(BigDecimal.ZERO, BigDecimal::add);
    final BigDecimal totalMarketValue =
        positionValuationList.stream().map(PositionValuation::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    final BigDecimal totalUnrealizedPnL =
        positionValuationList.stream().map(PositionValuation::unrealizedPnL).reduce(BigDecimal.ZERO, BigDecimal::add);
    final BigDecimal totalUnrealizedPnlPercent =
        totalUnrealizedPnL.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

    return new PortfolioSummary(totalInvested, totalMarketValue, totalUnrealizedPnL, totalUnrealizedPnlPercent);
  }

}
