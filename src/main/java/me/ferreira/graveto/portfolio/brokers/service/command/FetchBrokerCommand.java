package me.ferreira.graveto.portfolio.brokers.service.command;

import java.util.UUID;

public record FetchBrokerCommand(
    UUID sid
) {
}
