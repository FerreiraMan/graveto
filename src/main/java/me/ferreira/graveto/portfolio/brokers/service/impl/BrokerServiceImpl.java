package me.ferreira.graveto.portfolio.brokers.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.portfolio.BrokerNotFoundException;
import me.ferreira.graveto.identity.api.UserApi;
import me.ferreira.graveto.identity.api.UserResponseDto;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembership;
import me.ferreira.graveto.portfolio.brokers.domain.BrokerMembershipRole;
import me.ferreira.graveto.portfolio.brokers.domain.event.BrokerCreatedEvent;
import me.ferreira.graveto.portfolio.brokers.repository.BrokerRepository;
import me.ferreira.graveto.portfolio.brokers.service.BrokerService;
import me.ferreira.graveto.portfolio.brokers.service.command.CreateBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.command.FetchBrokerCommand;
import me.ferreira.graveto.portfolio.brokers.service.payload.BrokerDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class BrokerServiceImpl implements BrokerService {

  private final UserApi userApi;
  private final BrokerRepository brokerRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public BrokerDetails createBroker(final CreateBrokerCommand command) {

    final Broker broker = Broker.create(command.name(), command.accountSid(), command.currency());
    final BrokerMembership membership = BrokerMembership.create(command.userSid(), BrokerMembershipRole.OWNER);

    broker.addMembership(membership);

    final Broker createdBroker = brokerRepository.save(broker);

    log.info("Broker created successfully. BrokerSid: {}", createdBroker.getSid());
    eventPublisher.publishEvent(new BrokerCreatedEvent(createdBroker));

    return buildBrokerDetails(createdBroker);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Broker> fetchAllBrokers(final UUID userSid) {

    return brokerRepository.findAllByUserSid(userSid);
  }

  @Override
  @Transactional(readOnly = true)
  public BrokerDetails fetchBroker(final FetchBrokerCommand command) {

    final Broker broker = brokerRepository.findBySidAndUserSid(command.sid(), command.userSid())
        .orElseThrow(() -> new BrokerNotFoundException(command.sid()));

    return buildBrokerDetails(broker);
  }

  @Override
  @Transactional(readOnly = true)
  public Broker fetchBrokerEntity(final UUID brokerSid) {

    return brokerRepository.findBySid(brokerSid)
        .orElseThrow(() -> new BrokerNotFoundException(brokerSid));
  }

  private BrokerDetails buildBrokerDetails(final Broker broker) {

    final Set<UUID> userList = broker.getMemberships().stream()
        .map(BrokerMembership::getUserSid)
        .collect(Collectors.toSet());

    final Map<UUID, UserResponseDto> brokerUsersInfo = userApi.fetchUserDetailsByUserSids(userList);

    final List<BrokerDetails.MembershipDetails> membershipDetails = broker.getMemberships().stream()
        .map(m -> new BrokerDetails.MembershipDetails(
            m.getUserSid(),
            brokerUsersInfo.getOrDefault(m.getUserSid(), new UserResponseDto(m.getUserSid(), "")).email(),
            m.getRole().name()))
        .toList();

    return new BrokerDetails(broker.getSid(), broker.getName(), broker.getStatus(), broker.getCurrency(),
        broker.getAccountSid(), membershipDetails);
  }

}
