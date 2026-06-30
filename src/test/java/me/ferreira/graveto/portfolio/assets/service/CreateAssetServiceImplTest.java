package me.ferreira.graveto.portfolio.assets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.common.web.exception.portfolio.InvalidExchangeException;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.domain.event.AssetCreatedEvent;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.impl.AssetServiceImpl;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class CreateAssetServiceImplTest {

  @InjectMocks
  private AssetServiceImpl assetService;
  @Mock
  private StockExchangeService stockExchangeService;
  @Mock
  private AssetRepository assetRepository;
  @Mock
  private ApplicationEventPublisher publisher;

  @Test
  void shouldCreateAssetWhenItDoesNotExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final CreateAssetCommand command = new CreateAssetCommand(
        userSid, "iwda.as", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);

    final StockExchange stockExchange = new StockExchange();
    stockExchange.setSuffix(".AS");

    when(stockExchangeService.fetchStockExchange(any(FetchStockExchangeCommand.class))).thenReturn(stockExchange);
    when(assetRepository.findByTickerAndStockExchange("IWDA", stockExchange)).thenReturn(Optional.empty());
    when(assetRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    final Asset result = assetService.createAsset(command);

    // Assert
    final ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
    verify(assetRepository).save(captor.capture());

    final Asset savedAsset = captor.getValue();
    assertThat(savedAsset.getSid()).isNotNull();
    assertThat(savedAsset.getTicker()).isEqualTo("IWDA");
    assertThat(savedAsset.getName()).isEqualTo("iShares Core MSCI World");
    assertThat(savedAsset.getAssetType()).isEqualTo(AssetType.ETF);
    assertThat(savedAsset.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(savedAsset.getStockExchange()).isEqualTo(stockExchange);
    assertThat(result).isEqualTo(savedAsset);
    verify(publisher, times(1)).publishEvent(new AssetCreatedEvent(userSid, savedAsset, stockExchange.getSuffix()));
  }

  @Test
  void shouldReturnExistingAssetWhenAlreadyExists() {
    // Arrange
    final CreateAssetCommand command = new CreateAssetCommand(
        UUID.randomUUID(), "iwda.as", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);

    final StockExchange stockExchange = new StockExchange();
    stockExchange.setSuffix(".AS");

    final Asset existingAsset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);
    existingAsset.setStockExchange(stockExchange);

    when(stockExchangeService.fetchStockExchange(any(FetchStockExchangeCommand.class))).thenReturn(stockExchange);
    when(assetRepository.findByTickerAndStockExchange("IWDA", stockExchange)).thenReturn(Optional.of(existingAsset));

    // Act
    final Asset result = assetService.createAsset(command);

    // Assert
    verify(assetRepository, never()).save(any());
    assertThat(result).isEqualTo(existingAsset);
    verify(publisher, times(0)).publishEvent(any());
  }

  @Test
  void shouldThrowInvalidExchangeExceptionWhenSymbolHasNoSuffix() {
    // Arrange
    final CreateAssetCommand command = new CreateAssetCommand(
        UUID.randomUUID(), "aapl", "Apple Inc.", AssetType.EQUITY, Currency.USD);

    // Act & Assert
    assertThatThrownBy(() -> assetService.createAsset(command))
        .isInstanceOf(InvalidExchangeException.class);
  }

  @Test
  void shouldUppercaseTickerFromSymbol() {
    // Arrange
    final CreateAssetCommand command = new CreateAssetCommand(
        UUID.randomUUID(), "vwce.de", "Vanguard FTSE All-World", AssetType.ETF, Currency.EUR);

    final StockExchange stockExchange = new StockExchange();
    when(stockExchangeService.fetchStockExchange(any())).thenReturn(stockExchange);
    when(assetRepository.findByTickerAndStockExchange("VWCE", stockExchange)).thenReturn(Optional.empty());
    when(assetRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Act
    assetService.createAsset(command);

    // Assert
    final ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
    verify(assetRepository).save(captor.capture());
    assertThat(captor.getValue().getTicker()).isEqualTo("VWCE");
  }

}
