package me.ferreira.graveto.portfolio.assets.web.dto.response;

import java.util.UUID;

public record AssetResponseDto(
    UUID sid,
    String ticker,
    String name,
    String type,
    String currency,
    String exchangeLocation,
    String exchangeName
) {
}
