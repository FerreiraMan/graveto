package me.ferreira.graveto.portfolio.stockexchange.service;

import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;

public interface StockExchangeService {

  StockExchange fetchStockExchange(FetchStockExchangeCommand command);

}
