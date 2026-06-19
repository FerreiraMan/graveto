package me.ferreira.graveto.portfolio.utils;

import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;

public class BrokerTestFactory {

  public static Broker createBrokerWithOwner(final UUID userSid, final String name, final UUID accountSid) {

    final Broker broker = Broker.create(name, accountSid, Currency.EUR);
    broker.addMembership(BrokerMembership.create(userSid, BrokerMembershipRole.OWNER));
    return broker;
  }

}
