package me.ferreira.graveto.portfolio.brokers.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.event.BrokerCreatedEvent;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class BrokerServiceImpl implements BrokerService {

  private final BrokerRepository brokerRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public Broker createBroker(final CreateBrokerCommand command) {

    final Broker broker = Broker.create(command.name(), command.accountSid(), command.currency());
    final BrokerMembership membership = BrokerMembership.create(command.userSid(), BrokerMembershipRole.OWNER);

    broker.addMembership(membership);

    final Broker createdBroker = brokerRepository.save(broker);

    log.info("Broker created successfully. BrokerSid: {}", createdBroker.getSid());
    eventPublisher.publishEvent(new BrokerCreatedEvent(createdBroker));

    return createdBroker;
  }

  @Override
  @Transactional
  public Broker fetchBroker(final FetchBrokerCommand command) {
    return null;
  }

}
