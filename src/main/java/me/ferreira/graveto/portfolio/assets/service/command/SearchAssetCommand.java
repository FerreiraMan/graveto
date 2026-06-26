package me.ferreira.graveto.portfolio.assets.service.command;

import java.util.UUID;

public record SearchAssetCommand(
    UUID userSid,
    String keyword
) {
}
