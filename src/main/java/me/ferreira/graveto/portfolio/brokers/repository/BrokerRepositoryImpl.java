package me.ferreira.graveto.portfolio.brokers.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class BrokerRepositoryImpl implements BrokerRepository {

  private final BrokerJpaRepository repository;

  @Override
  public Broker save(final Broker broker) {
    return repository.save(broker);
  }

  @Override
  public List<Broker> saveAll(final List<Broker> brokers) {
    return repository.saveAll(brokers);
  }

  @Override
  public List<Broker> findAllByUserSid(final UUID userSid) {
    return repository.findAllByUserSid(userSid);
  }

  @Override
  public Optional<Broker> findBySidAndUserSid(final UUID sid, final UUID userSid) {
    return repository.findBySidAndUserSid(sid, userSid);
  }

  @Override
  public Optional<Broker> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

}
