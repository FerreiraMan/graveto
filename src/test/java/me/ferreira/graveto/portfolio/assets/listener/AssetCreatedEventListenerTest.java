package me.ferreira.graveto.portfolio.assets.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.QuoteDataResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.domain.event.AssetCreatedEvent;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssetCreatedEventListenerTest {

  @InjectMocks
  private AssetCreatedEventListener listener;
  @Mock
  private MarketDataClient marketDataClient;
  @Mock
  private AssetRepository assetRepository;

  @Test
  void shouldEnrichAssetWithCurrentPrice() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".AS");

    final BigDecimal expectedPrice = new BigDecimal("89.45");
    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "ETF", expectedPrice,
        null, null, null, null, "EUR", null, null, null);

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of(quote));
    when(assetRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Asset result = listener.onAssetCreation(event);

    // Assert
    final ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
    verify(assetRepository).save(captor.capture());
    assertThat(captor.getValue().getCurrentPrice()).isEqualByComparingTo(expectedPrice);
    assertThat(result.getCurrentPrice()).isEqualByComparingTo(expectedPrice);
  }

  @Test
  void shouldNotSaveWhenNoMatchingQuoteFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".AS");

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of());

    // Act
    final Asset result = listener.onAssetCreation(event);

    // Assert
    verify(assetRepository, never()).save(any());
    assertThat(result.getCurrentPrice()).isNull();
  }

  @Test
  void shouldNotSaveWhenQuoteCurrencyDoesNotMatch() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".AS");

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "ETF", new BigDecimal("89.45"),
        null, null, null, null, "USD", null, null, null);

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final Asset result = listener.onAssetCreation(event);

    // Assert
    verify(assetRepository, never()).save(any());
    assertThat(result.getCurrentPrice()).isNull();
  }

  @Test
  void shouldNotSaveWhenQuoteTypeDoesNotMatch() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".AS");

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "IWDA.AS", "iShares Core MSCI World", "EQUITY", new BigDecimal("89.45"),
        null, null, null, null, "EUR", null, null, null);

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final Asset result = listener.onAssetCreation(event);

    // Assert
    verify(assetRepository, never()).save(any());
    assertThat(result.getCurrentPrice()).isNull();
  }

  @Test
  void shouldNotSaveWhenQuoteSymbolDoesNotMatch() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".AS");

    final QuoteDataResponseDto.Result quote = new QuoteDataResponseDto.Result(
        "VWCE.AS", "Vanguard FTSE All-World", "ETF", new BigDecimal("100.00"),
        null, null, null, null, "EUR", null, null, null);

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of(quote));

    // Act
    final Asset result = listener.onAssetCreation(event);

    // Assert
    verify(assetRepository, never()).save(any());
    assertThat(result.getCurrentPrice()).isNull();
  }

  @Test
  void shouldConstructSymbolWithUppercaseTickerAndSuffix() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final Asset asset = Asset.create("iwda", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    final AssetCreatedEvent event = new AssetCreatedEvent(userSid, asset, ".as");

    when(marketDataClient.fetchQuoteData(userSid, List.of("IWDA.AS"))).thenReturn(List.of());

    // Act
    listener.onAssetCreation(event);

    // Assert
    verify(marketDataClient).fetchQuoteData(userSid, List.of("IWDA.AS"));
  }

}
