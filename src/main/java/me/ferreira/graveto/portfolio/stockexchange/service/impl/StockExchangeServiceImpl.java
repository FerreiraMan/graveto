package me.ferreira.graveto.portfolio.stockexchange.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.portfolio.StockExchangeNotFoundException;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.repository.StockExchangeRepository;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StockExchangeServiceImpl implements StockExchangeService {

  private final StockExchangeRepository stockExchangeRepository;

  @Override
  @Transactional(readOnly = true)
  public StockExchange fetchStockExchange(final FetchStockExchangeCommand command) {

    return stockExchangeRepository.findBySuffix(command.suffix())
        .orElseThrow(() -> new StockExchangeNotFoundException(command.suffix()));
  }

}
