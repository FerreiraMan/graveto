package me.ferreira.graveto.portfolio.positions.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionResponseDto(
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