package me.ferreira.graveto.portfolio.orders.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.orders.domain.OrderType;

public record CreateOrderRequestDto(
    @NotNull(message = "Broker must be specified")
    UUID brokerSid,

    @NotNull(message = "Asset must be specified")
    UUID assetSid,

    @NotNull(message = "Order type is required")
    OrderType orderType,

    @NotNull
    @Positive(message = "Quantity must be a positive number")
    BigDecimal quantity,

    @NotNull
    @Positive(message = "Price must be a positive number")
    BigDecimal price,

    BigDecimal fees,

    @NotNull(message = "Currency is required")
    Currency currency,

    LocalDateTime executedAt,

    String notes
) {
}
