package me.ferreira.graveto.portfolio.assets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.scheduler.payload.AssetPriceUpdateResult;
import me.ferreira.graveto.portfolio.assets.service.impl.AssetServiceImpl;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class UpdateAssetPriceServiceImplTest {

  @InjectMocks
  private AssetServiceImpl assetService;
  @Mock
  private MarketDataClient marketDataClient;
  @Mock
  private AssetRepository assetRepository;
  @Mock
  private StockExchangeService stockExchangeService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  void shouldReturnEmptyListIfNoAssetIsFound() {
    // Arrange
    when(assetRepository.findAll()).thenReturn(List.of());

    // Act
    final List<AssetPriceUpdateResult> result = assetService.updateAssetPrices();

    // Assert
    assertThat(result).isEmpty();
    verify(marketDataClient, never()).fetchQuoteData(any(), any());
    verify(assetRepository, never()).saveAll(any());
  }

  @Test
  void shouldUpdateAssetPriceWhenQuoteMatches() {
    // Arrange
    final Asset asset = buildAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    when(assetRepository.findAll()).thenReturn(List.of(asset));

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "ETF", new BigDecimal("89.45"),
        null, null, null, null, "EUR", null, null, null);
    when(marketDataClient.fetchQuoteData(null, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().updated()).isTrue();
    assertThat(results.getFirst().symbol()).isEqualTo("IWDA.AS");
    assertThat(asset.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));

    final ArgumentCaptor<List<Asset>> captor = ArgumentCaptor.forClass(List.class);
    verify(assetRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
  }

  @Test
  void shouldSkipAssetWhenNoQuoteDataReturned() {
    // Arrange
    final Asset asset = buildAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    when(assetRepository.findAll()).thenReturn(List.of(asset));
    when(marketDataClient.fetchQuoteData(null, List.of("IWDA.AS"))).thenReturn(List.of());

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().updated()).isFalse();
    assertThat(asset.getCurrentPrice()).isNull();

    final ArgumentCaptor<List<Asset>> captor = ArgumentCaptor.forClass(List.class);
    verify(assetRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  void shouldSkipAssetWhenCurrencyDoesNotMatch() {
    // Arrange
    final Asset asset = buildAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    when(assetRepository.findAll()).thenReturn(List.of(asset));

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "ETF", new BigDecimal("89.45"),
        null, null, null, null, "USD", null, null, null);
    when(marketDataClient.fetchQuoteData(null, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().updated()).isFalse();
    assertThat(asset.getCurrentPrice()).isNull();
  }

  @Test
  void shouldSkipAssetWhenQuoteTypeDoesNotMatch() {
    // Arrange
    final Asset asset = buildAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    when(assetRepository.findAll()).thenReturn(List.of(asset));

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "EQUITY", new BigDecimal("89.45"),
        null, null, null, null, "EUR", null, null, null);
    when(marketDataClient.fetchQuoteData(null, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().updated()).isFalse();
    assertThat(asset.getCurrentPrice()).isNull();
  }

  @Test
  void shouldUpdateMultipleAssetsAndSkipNonMatching() {
    // Arrange
    final Asset iwda = buildAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    final Asset vwce = buildAsset("VWCE", ".DE", AssetType.ETF, Currency.EUR);
    final Asset missing = buildAsset("FAKE", ".XX", AssetType.ETF, Currency.EUR);
    when(assetRepository.findAll()).thenReturn(List.of(iwda, vwce, missing));

    final QuoteDataResponseDto.Result iwdaQuote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares", "ETF", new BigDecimal("89.45"),
        null, null, null, null, "EUR", null, null, null);
    final QuoteDataResponseDto.Result vwceQuote = new QuoteDataResponseDto.Result(
        "VWCE.DE", "Vanguard", "ETF", new BigDecimal("105.20"),
        null, null, null, null, "EUR", null, null, null);
    when(marketDataClient.fetchQuoteData(null, List.of("IWDA.AS", "VWCE.DE", "FAKE.XX")))
        .thenReturn(List.of(iwdaQuote, vwceQuote));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(3);

    final List<AssetPriceUpdateResult> updated = results.stream().filter(AssetPriceUpdateResult::updated).toList();
    final List<AssetPriceUpdateResult> failed = results.stream().filter(r -> !r.updated()).toList();

    assertThat(updated).hasSize(2);
    assertThat(failed).hasSize(1);
    assertThat(failed.getFirst().symbol()).isEqualTo("FAKE.XX");

    assertThat(iwda.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));
    assertThat(vwce.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("105.20"));
    assertThat(missing.getCurrentPrice()).isNull();

    final ArgumentCaptor<List<Asset>> captor = ArgumentCaptor.forClass(List.class);
    verify(assetRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
  }

  private static Asset buildAsset(final String ticker, final String suffix, final AssetType type,
                                  final Currency currency) {
    final StockExchange exchange = new StockExchange();
    exchange.setSuffix(suffix);

    final Asset asset = Asset.create(ticker, "Test Asset", type, currency);
    asset.setStockExchange(exchange);
    return asset;
  }

}
