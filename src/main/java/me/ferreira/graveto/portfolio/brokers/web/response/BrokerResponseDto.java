package me.ferreira.graveto.portfolio.brokers.web.response;

import java.util.UUID;

public record BrokerResponseDto(
    UUID sid,
    String name,
    String status
) {
}
