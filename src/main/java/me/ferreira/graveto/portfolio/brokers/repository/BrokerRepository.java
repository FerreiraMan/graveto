package me.ferreira.graveto.portfolio.brokers.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;

public interface BrokerRepository {

  Broker save(Broker broker);

  List<Broker> saveAll(List<Broker> brokers);

  List<Broker> findAllByUserSid(UUID userSid);

  Optional<Broker> findBySidAndUserSid(UUID sid, UUID userSid);

  Optional<Broker> findBySid(UUID sid);

}
