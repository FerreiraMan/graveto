package me.ferreira.graveto.portfolio.positions.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PositionJpaRepository extends JpaRepository<Position, Long> {

  @Query("SELECT p FROM Position p WHERE p.broker.sid = ?1 AND p.asset.sid = ?2")
  Optional<Position> findByBrokerSidAndAssetSid(final UUID brokerSid, final UUID assetSid);

}
