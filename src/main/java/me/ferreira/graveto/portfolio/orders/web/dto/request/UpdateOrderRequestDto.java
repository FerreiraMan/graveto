package me.ferreira.graveto.portfolio.orders.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateOrderRequestDto(
    @NotNull(message = "Order must be specified")
    UUID orderSid,

    @Positive(message = "Quantity must be a positive number")
    BigDecimal quantity,

    @Positive(message = "Price must be a positive number")
    BigDecimal price,

    BigDecimal fees,
    LocalDateTime executedAt,
    String notes
) {
}
