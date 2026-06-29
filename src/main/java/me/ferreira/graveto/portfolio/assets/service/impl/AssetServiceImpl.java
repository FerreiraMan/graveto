package me.ferreira.graveto.portfolio.assets.service.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.portfolio.AssetNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InvalidExchangeException;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final MarketDataClient marketDataClient;
  private final StockExchangeService stockExchangeService;
  private final AssetRepository assetRepository;

  @Override
  public List<AssetSearchRecommendation> searchAsset(final SearchAssetCommand command) {

    final List<SearchAssetResponseDto.Result> assetSearchResponse =
        marketDataClient.searchAsset(command.userSid(), command.keyword());

    return assetSearchResponse.stream()
        .map(result -> new AssetSearchRecommendation(result.symbol(), result.name(), result.type(), result.exchange()))
        .toList();
  }

  // TODO: revisit this flow to authorize request and handle stock exchanges without suffix (US markets).
  @Override
  @Transactional
  public Asset createAsset(final CreateAssetCommand command) {

    final String[] assetRepresentation = command.symbol().split("\\.");

    if (assetRepresentation.length < 2) {
      throw new InvalidExchangeException();
    }

    final String ticker = assetRepresentation[0].toUpperCase();
    final String suffix = "." + assetRepresentation[1];

    final StockExchange stockExchange =
        stockExchangeService.fetchStockExchange(new FetchStockExchangeCommand(suffix));

    return assetRepository.findByTickerAndStockExchange(ticker, stockExchange)
        .orElseGet(() -> {

          final Asset asset = Asset.create(ticker, command.name(), command.type(), command.currency());
          asset.setStockExchange(stockExchange);
          return assetRepository.save(asset);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public Asset fetchAsset(final FetchAssetCommand command) {

    return assetRepository.findBySid(command.sid())
        .orElseThrow(() -> new AssetNotFoundException(command.sid()));
  }

}
