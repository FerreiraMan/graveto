package me.ferreira.graveto.portfolio.brokers.domain.event;

import me.ferreira.graveto.portfolio.brokers.domain.Broker;

public record BrokerCreatedEvent(
    Broker broker
) {
}
