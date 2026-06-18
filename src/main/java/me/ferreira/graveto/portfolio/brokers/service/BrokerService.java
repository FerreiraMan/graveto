package me.ferreira.graveto.portfolio.brokers.service;

import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;

public interface BrokerService {

  Broker fetchBroker(FetchBrokerCommand command);

}
