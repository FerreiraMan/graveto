package me.ferreira.graveto.portfolio.brokers.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;

public record CreateBrokerRequestDto(
    UUID accountSid,

    @NotBlank(message = "Name cannot be empty.")
    String name,

    @NotNull(message = "Currency is required")
    Currency currency
) {
}
