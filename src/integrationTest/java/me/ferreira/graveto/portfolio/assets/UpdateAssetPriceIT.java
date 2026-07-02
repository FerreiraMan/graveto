package me.ferreira.graveto.portfolio.assets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.ferreira.graveto.common.domain.Currency;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.domain.AssetType;
import me.ferreira.graveto.portfolio.assets.repository.AssetRepository;
import me.ferreira.graveto.portfolio.assets.scheduler.payload.AssetPriceUpdateResult;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.config.PortfolioBaseIntegrationTest;
import me.ferreira.graveto.portfolio.stockexchange.domain.StockExchange;
import me.ferreira.graveto.portfolio.stockexchange.service.StockExchangeService;
import me.ferreira.graveto.portfolio.stockexchange.service.command.FetchStockExchangeCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/portfolio/sql/delete_all.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdateAssetPriceIT extends PortfolioBaseIntegrationTest {

  @Autowired
  private AssetService assetService;
  @Autowired
  private AssetRepository assetRepository;
  @Autowired
  private StockExchangeService stockExchangeService;
  @Autowired
  private MockServerClient mockServerClient;

  @AfterEach
  void resetMockServer() {
    mockServerClient.reset();
  }

  @Test
  void shouldUpdateAssetPrices() {
    // Arrange
    final Asset iwda = setupAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    final Asset vwce = setupAsset("VWCE", ".DE", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote")
            .withQueryStringParameter("symbols", "VWCE.DE,IWDA.AS"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[
                  {"symbol":"IWDA.AS","longName":"iShares Core MSCI World","quoteType":"ETF","regularMarketPrice":89.45,"currency":"EUR"},
                  {"symbol":"VWCE.DE","longName":"Vanguard FTSE All-World","quoteType":"ETF","regularMarketPrice":105.20,"currency":"EUR"}
                ]}}
                """));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(2);
    assertThat(results).allMatch(AssetPriceUpdateResult::updated);

    final Optional<Asset> updatedIwda = assetRepository.findBySid(iwda.getSid());
    assertThat(updatedIwda).isPresent();
    assertThat(updatedIwda.get().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));

    final Optional<Asset> updatedVwce = assetRepository.findBySid(vwce.getSid());
    assertThat(updatedVwce).isPresent();
    assertThat(updatedVwce.get().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("105.20"));
  }

  @Test
  void shouldMarkAssetAsFailedWhenNoQuoteReturned() {
    // Arrange
    setupAsset("FAKE", ".MX", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[]}}
                """));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().updated()).isFalse();
  }

  @Test
  void shouldReturnEmptyWhenNoAssetsExist() {
    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    assertThat(results).isEmpty();
  }

  @Test
  void shouldPartiallyUpdateWhenSomeAssetsMatchAndOthersDont() {
    // Arrange
    final Asset iwda = setupAsset("IWDA", ".AS", AssetType.ETF, Currency.EUR);
    final Asset mismatched = setupAsset("VWCE", ".DE", AssetType.ETF, Currency.EUR);

    mockServerClient
        .when(request()
            .withMethod("GET")
            .withPath("/v6/finance/quote"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("""
                {"quoteResponse":{"result":[
                  {"symbol":"IWDA.AS","longName":"iShares","quoteType":"ETF","regularMarketPrice":89.45,"currency":"EUR"},
                  {"symbol":"VWCE.DE","longName":"Vanguard","quoteType":"ETF","regularMarketPrice":105.20,"currency":"USD"}
                ]}}
                """));

    // Act
    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    // Assert
    final List<AssetPriceUpdateResult> updated = results.stream().filter(AssetPriceUpdateResult::updated).toList();
    final List<AssetPriceUpdateResult> failed = results.stream().filter(r -> !r.updated()).toList();

    assertThat(updated).hasSize(1);
    assertThat(updated.getFirst().symbol()).isEqualTo("IWDA.AS");
    assertThat(failed).hasSize(1);
    assertThat(failed.getFirst().symbol()).isEqualTo("VWCE.DE");

    final Optional<Asset> updatedIwda = assetRepository.findBySid(iwda.getSid());
    assertThat(updatedIwda.get().getCurrentPrice()).isEqualByComparingTo(new BigDecimal("89.45"));

    final Optional<Asset> unchangedVwce = assetRepository.findBySid(mismatched.getSid());
    assertThat(unchangedVwce.get().getCurrentPrice()).isNull();
  }

  private Asset setupAsset(final String ticker, final String suffix, final AssetType type, final Currency currency) {
    final StockExchange exchange = stockExchangeService.fetchStockExchange(new FetchStockExchangeCommand(suffix));
    final Asset asset = Asset.create(ticker, "Test " + ticker, type, currency);
    asset.setStockExchange(exchange);
    return assetRepository.save(asset);
  }

}
