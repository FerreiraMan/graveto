package me.ferreira.graveto.portfolio.positions.service.command;

import java.util.UUID;

public record FetchPositionOverviewCommand(
    UUID userSid,
    UUID brokerSid
) {
}
