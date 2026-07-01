package me.ferreira.graveto.portfolio.positions.repository;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.positions.domain.Position;

public interface PositionRepository {

  Position save(Position position);

  Optional<Position> findByBrokerSidAndAssetSid(UUID brokerSid, UUID assetSid);

}
