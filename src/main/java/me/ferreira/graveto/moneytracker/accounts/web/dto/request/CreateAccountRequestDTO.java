package me.ferreira.graveto.moneytracker.accounts.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import me.ferreira.graveto.common.domain.Currency;

import java.math.BigDecimal;

public record CreateAccountRequestDTO(
        @NotNull
        @PositiveOrZero(message = "Initial balance cannot be negative")
        BigDecimal initialBalance,

        @NotNull(message = "Currency is required")
        Currency currency,

        @NotBlank(message = "Institution name cannot be empty")
        String institution
) {}
