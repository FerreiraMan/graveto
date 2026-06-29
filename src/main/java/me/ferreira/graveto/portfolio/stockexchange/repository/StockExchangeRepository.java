package me.ferreira.graveto.portfolio.stockexchange.repository;

import java.util.Optional;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;

public interface StockExchangeRepository {

  Optional<StockExchange> findBySuffix(String suffix);

}
