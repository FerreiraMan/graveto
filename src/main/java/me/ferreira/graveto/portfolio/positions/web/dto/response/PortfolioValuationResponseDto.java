package me.ferreira.graveto.portfolio.positions.web.dto.response;

import java.math.BigDecimal;

public record PortfolioValuationResponseDto(
    BigDecimal totalInvested,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedPnL,
    BigDecimal totalUnrealizedPnlPercent
) {
}
