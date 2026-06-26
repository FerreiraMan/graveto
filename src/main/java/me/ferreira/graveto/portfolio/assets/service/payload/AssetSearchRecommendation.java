package me.ferreira.graveto.portfolio.assets.service.payload;

public record AssetSearchRecommendation(
    String ticker,
    String name,
    String type,
    String exchange
) {
}
