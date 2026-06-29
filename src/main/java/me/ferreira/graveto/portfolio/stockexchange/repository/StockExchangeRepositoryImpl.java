package me.ferreira.graveto.portfolio.stockexchange.repository;

import java.util.Optional;
import lombok.AllArgsConstructor;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class StockExchangeRepositoryImpl implements StockExchangeRepository {

  private final StockExchangeJpaRepository repository;

  @Override
  public Optional<StockExchange> findBySuffix(final String suffix) {
    return repository.findBySuffix(suffix);
  }

}
