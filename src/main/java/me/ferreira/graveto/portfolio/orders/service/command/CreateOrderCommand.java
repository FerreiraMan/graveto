package me.ferreira.graveto.portfolio.orders.service.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;

public record CreateOrderCommand(
    UUID userSid,
    UUID brokerSid,
    UUID assetSid,
    OrderType orderType,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fees,
    Currency currency,
    LocalDateTime executedAt,
    String notes
) {
}
