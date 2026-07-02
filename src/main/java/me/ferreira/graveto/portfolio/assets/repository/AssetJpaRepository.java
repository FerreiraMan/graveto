package me.ferreira.graveto.portfolio.assets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.Asset_;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetJpaRepository extends JpaRepository<Asset, Long> {

  @EntityGraph(attributePaths = {Asset_.STOCK_EXCHANGE})
  Optional<Asset> findBySid(final UUID sid);

  @Query("SELECT a FROM Asset a JOIN FETCH a.stockExchange se JOIN FETCH se.country " +
      "WHERE a.ticker = ?1 AND a.stockExchange = ?2")
  Optional<Asset> findByTickerAndStockExchange(final String ticker, final StockExchange stockExchange);

  @EntityGraph(attributePaths = {Asset_.STOCK_EXCHANGE})
  List<Asset> findAll();

}
