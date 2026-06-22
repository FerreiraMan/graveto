package me.ferreira.graveto.portfolio.brokers.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.brokers.domain.Broker;
import me.ferreira.graveto.portfolio.brokers.domain.Broker_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrokerJpaRepository extends JpaRepository<Broker, Long> {

  @Query(value = "SELECT b FROM Broker b JOIN FETCH b.memberships WHERE b.sid = ?1 AND EXISTS " +
      "(SELECT 1 FROM BrokerMembership bm WHERE bm.broker = b AND bm.userSid = ?2)")
  Optional<Broker> findBySidAndUserSid(final UUID sid, final UUID userSid);

  @Query(value = "SELECT b FROM Broker b JOIN b.memberships bm WHERE bm.userSid = ?1")
  List<Broker> findAllByUserSid(final UUID userSid);

  @EntityGraph(attributePaths = {Broker_.MEMBERSHIPS})
  Optional<Broker> findBySid(final UUID sid);

}
