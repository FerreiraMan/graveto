package me.ferreira.graveto.portfolio.positions.service.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionValuation(
    UUID assetSid,
    String ticker,
    BigDecimal quantity,
    BigDecimal averageCost,
    BigDecimal totalInvested,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    BigDecimal unrealizedPnL,
    BigDecimal unrealizedPnlPercent
) {
}
