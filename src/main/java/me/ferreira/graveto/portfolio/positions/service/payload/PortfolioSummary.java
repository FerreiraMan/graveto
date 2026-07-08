package me.ferreira.graveto.portfolio.positions.service.payload;

import java.math.BigDecimal;

public record PortfolioSummary(
    BigDecimal totalInvested,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedPnL,
    BigDecimal totalUnrealizedPnlPercent
) {
}
