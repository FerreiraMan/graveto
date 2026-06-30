package me.ferreira.graveto.portfolio.assets.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.event.AssetCreatedEvent;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetCreatedEventListener {

  private final MarketDataClient marketDataClient;
  private final AssetRepository assetRepository;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Asset onAssetCreation(final AssetCreatedEvent event) {

    final Asset asset = event.createdAsset();

    final String symbol = asset.getTicker().toUpperCase() + event.suffix().toUpperCase();

    final List<QuoteDataResponseDto.Result> quoteDataResults =
        marketDataClient.fetchQuoteData(event.userSid(), List.of(symbol));

    final Optional<QuoteDataResponseDto.Result> targetQuote = quoteDataResults.stream()
        .filter(result -> asset.getCurrency().name().equals(result.currency()))
        .filter(result -> asset.getAssetType().name().equals(result.quoteType()))
        .filter(result -> symbol.equals(result.symbol()))
        .findFirst();

    if (targetQuote.isEmpty()) {
      log.warn("No quote data of asset [{}] was found using the following symbol: {}", asset.getSid(), symbol);
      return asset;
    }

    asset.setCurrentPrice(targetQuote.get().regularMarketPrice());
    return assetRepository.save(asset);
  }

}
