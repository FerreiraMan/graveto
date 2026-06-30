package me.ferreira.graveto.portfolio.assets.domain.event;

import java.util.UUID;
import me.ferreira.graveto.portfolio.assets.domain.Asset;

public record AssetCreatedEvent(
    UUID userSid,
    Asset createdAsset,
    String suffix
) {
}
