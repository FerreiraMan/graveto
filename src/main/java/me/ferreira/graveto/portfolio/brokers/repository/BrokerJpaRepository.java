package me.ferreira.graveto.portfolio.brokers.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.Broker_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerJpaRepository extends JpaRepository<Broker, Long> {

  @EntityGraph(attributePaths = {Broker_.MEMBERSHIPS})
  Optional<Broker> findBySid(final UUID sid);

}
