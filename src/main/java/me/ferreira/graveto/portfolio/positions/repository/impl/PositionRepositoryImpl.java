package me.ferreira.graveto.portfolio.positions.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.portfolio.positions.domain.Position;
import me.ferreira.graveto.portfolio.positions.repository.PositionJpaRepository;
import me.ferreira.graveto.portfolio.positions.repository.PositionRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PositionRepositoryImpl implements PositionRepository {

  private final PositionJpaRepository repository;

  @Override
  public Position save(final Position position) {
    return repository.save(position);
  }

  @Override
  public Optional<Position> findByBrokerSidAndAssetSid(final UUID brokerSid, final UUID assetSid) {
    return repository.findByBrokerSidAndAssetSid(brokerSid, assetSid);
  }

  @Override
  public List<Position> fetchAllByBrokerSidWithAsset(final UUID brokerSid) {
    return repository.fetchAllByBrokerSidWithAsset(brokerSid);
  }

}
