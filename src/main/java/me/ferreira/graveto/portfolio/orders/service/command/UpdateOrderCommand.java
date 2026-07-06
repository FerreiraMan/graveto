package me.ferreira.graveto.portfolio.orders.service.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateOrderCommand(
    UUID userSid,
    UUID sid,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fees,
    LocalDateTime executedAt,
    String notes
) {
}
