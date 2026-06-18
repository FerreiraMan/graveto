package me.ferreira.graveto.portfolio.assets.service.command;

import java.util.UUID;

public record FetchAssetCommand(
    UUID sid
) {
}
