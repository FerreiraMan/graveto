package me.ferreira.graveto.portfolio.brokers.service.command;

import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;

public record AddMemberToBrokerCommand(
    UUID userSid,
    UUID brokerSid,
    String email,
    BrokerMembershipRole role
) {
}
