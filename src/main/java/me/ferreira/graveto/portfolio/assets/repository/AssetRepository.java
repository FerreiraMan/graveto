package me.ferreira.graveto.portfolio.assets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;

public interface AssetRepository {

  Asset save(Asset asset);

  List<Asset> saveAll(List<Asset> assetList);

  Optional<Asset> findBySid(UUID sid);

  Optional<Asset> findByTickerAndStockExchange(String ticker, StockExchange stockExchange);

}
