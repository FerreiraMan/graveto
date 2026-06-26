package me.ferreira.graveto.portfolio.assets.web.dto.response;

public record AssetRecommendationResponseDto(
    String ticker,
    String name,
    String type,
    String exchange
) {
}
