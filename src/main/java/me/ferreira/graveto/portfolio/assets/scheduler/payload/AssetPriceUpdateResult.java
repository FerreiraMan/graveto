package me.ferreira.graveto.portfolio.assets.scheduler.payload;

public record AssetPriceUpdateResult(
    String symbol,
    boolean updated
) {
}
