package me.ferreira.graveto.portfolio.positions.service.command;

import java.util.UUID;

public record FetchPortfolioOverviewCommand(
    UUID userSid,
    UUID brokerSid
) {
}
