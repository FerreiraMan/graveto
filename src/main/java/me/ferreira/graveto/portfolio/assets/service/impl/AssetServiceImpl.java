package me.ferreira.graveto.portfolio.assets.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.common.web.exception.portfolio.AssetNotFoundException;
import me.ferreira.graveto.common.web.exception.portfolio.InvalidExchangeException;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.event.AssetCreatedEvent;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.scheduler.payload.AssetPriceUpdateResult;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final MarketDataClient marketDataClient;
  private final StockExchangeService stockExchangeService;
  private final AssetRepository assetRepository;
  private final ApplicationEventPublisher eventPublisher;

  private record PriceMatchResult(List<Asset> updatedAssets, List<AssetPriceUpdateResult> results) {
  }

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
          final Asset savedAsset = assetRepository.save(asset);
          eventPublisher.publishEvent(
              new AssetCreatedEvent(command.userSid(), savedAsset, stockExchange.getSuffix()));
          return savedAsset;
        });
  }

  @Override
  @Transactional(readOnly = true)
  public Asset fetchAsset(final FetchAssetCommand command) {

    return assetRepository.findBySid(command.sid())
        .orElseThrow(() -> new AssetNotFoundException(command.sid()));
  }

  @Override
  @Transactional
  public List<AssetPriceUpdateResult> updateAssetPrices() {

    final List<Asset> assetList = assetRepository.findAll();

    if (assetList.isEmpty()) {
      return List.of();
    }

    final List<String> symbolList = assetList.stream()
        .map(a -> a.getTicker().toUpperCase() + a.getStockExchange().getSuffix().toUpperCase())
        .toList();

    final Map<String, QuoteDataResponseDto.Result> assetResultMap =
        marketDataClient.fetchQuoteData(null, symbolList).stream()
            .collect(Collectors.toMap(result -> result.symbol().toUpperCase(), result -> result));

    final PriceMatchResult mappedResults = matchAndApplyPrices(assetList, assetResultMap);

    assetRepository.saveAll(mappedResults.updatedAssets);
    return mappedResults.results;
  }

  private PriceMatchResult matchAndApplyPrices(final List<Asset> assetList,
                                               final Map<String, QuoteDataResponseDto.Result> assetResultMap) {

    final List<Asset> assetsToUpdate = new ArrayList<>();
    final List<AssetPriceUpdateResult> assetPriceUpdateResults = new ArrayList<>();

    for (final Asset asset : assetList) {

      final String symbol = asset.getTicker() + asset.getStockExchange().getSuffix();

      final QuoteDataResponseDto.Result result = assetResultMap.getOrDefault(symbol.toUpperCase(), null);

      if (result == null || !asset.getAssetType().name().equalsIgnoreCase(result.quoteType())
          || !asset.getCurrency().name().equalsIgnoreCase(result.currency())) {

        log.warn(
            "Skipping price update for asset [{}]. Type: [{} - {}], Currency [{} - {}]. No matching quote data found.",
            symbol, asset.getAssetType().name(), Objects.isNull(result) ? "" : result.quoteType(),
            asset.getCurrency().name(), Objects.isNull(result) ? "" : result.currency());

        assetPriceUpdateResults.add(new AssetPriceUpdateResult(symbol, false));
        continue;
      }

      asset.setCurrentPrice(result.regularMarketPrice());
      assetsToUpdate.add(asset);
      assetPriceUpdateResults.add(new AssetPriceUpdateResult(symbol, true));
    }

    return new PriceMatchResult(assetsToUpdate, assetPriceUpdateResults);
  }

}
