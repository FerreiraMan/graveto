package me.ferreira.graveto.portfolio.brokers.service.command;

import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;

public record CreateBrokerCommand(
    UUID userSid,
    UUID accountSid,
    String name,
    Currency currency
) {
}
