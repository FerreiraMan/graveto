package me.ferreira.graveto.portfolio.stockexchange.repository;

import java.util.Optional;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockExchangeJpaRepository extends JpaRepository<StockExchange, Long> {

  @Query(value = "SELECT s FROM StockExchange s JOIN FETCH s.country WHERE UPPER(s.suffix) = UPPER(?1)")
  Optional<StockExchange> findBySuffix(final String suffix);

}
