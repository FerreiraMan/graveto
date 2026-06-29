package me.ferreira.graveto.portfolio.assets.service.command;

import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;

public record CreateAssetCommand(
    UUID userSid,
    String symbol,
    String name,
    AssetType type,
    Currency currency
) {
}
