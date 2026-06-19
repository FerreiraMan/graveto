package me.ferreira.graveto.portfolio.brokers.service;

import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;

public interface BrokerService {

  BrokerDetails createBroker(CreateBrokerCommand command);

  BrokerDetails fetchBroker(FetchBrokerCommand command);

  Broker fetchBrokerEntity(UUID brokerSid);

}
