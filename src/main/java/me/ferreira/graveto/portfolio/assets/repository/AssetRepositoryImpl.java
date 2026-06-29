package me.ferreira.graveto.portfolio.assets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class AssetRepositoryImpl implements AssetRepository {

  private final AssetJpaRepository repository;

  @Override
  public Asset save(final Asset asset) {
    return repository.save(asset);
  }

  @Override
  public List<Asset> saveAll(final List<Asset> assetList) {
    return repository.saveAll(assetList);
  }

  @Override
  public Optional<Asset> findBySid(final UUID sid) {
    return repository.findBySid(sid);
  }

  @Override
  public Optional<Asset> findByTickerAndStockExchange(final String ticker, final StockExchange stockExchange) {
    return repository.findByTickerAndStockExchange(ticker, stockExchange);
  }

}
