package me.ferreira.graveto.portfolio.assets.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;

public record CreateAssetRequestDto(
    @NotBlank(message = "Symbol cannot be empty")
    String symbol,

    @NotBlank(message = "Name cannot be empty")
    String name,

    @NotNull(message = "Asset type is required")
    AssetType type,

    @NotNull(message = "Currency is required")
    Currency currency
) {
}
